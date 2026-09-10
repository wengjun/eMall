# 071 如何采集和解释 JVM 指标？

[返回按分类学习面试题](../README.md)

## Java 自带的采集接口

JMX/MXBean 是 JVM 暴露运行状态的标准入口，Micrometer 可以把这些信息转换成监控指标。
不再展开告警体系设计，先知道数据从哪里来、包含什么。

```java
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;

var memory = ManagementFactory.getMemoryMXBean();
System.out.println(memory.getHeapMemoryUsage());
System.out.println(memory.getNonHeapMemoryUsage());
System.out.println(ManagementFactory.getThreadMXBean().getThreadCount());

for (var pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
    System.out.println(pool.getName() + ": " + pool.getMemoryUsed());
}
```

Heap usage 不包含线程栈；non-heap usage 也不是“全部堆外内存”。
BufferPoolMXBean 主要覆盖 JDK 管理的缓冲池，不能据此断言已经统计所有 native 分配。

## Spring Boot 接入

加入 Actuator 与 `micrometer-registry-prometheus` 后，可以在受保护的管理端口暴露 prometheus 端点。
先在 `/actuator/metrics` 查实际 meter 名称，再看 Prometheus 的导出名称和标签。
不要把订单 ID、用户 ID 放进 JVM 指标标签。

重点区分当前使用量、已提交量、最大值和 GC 后存活量。
某些 MXBean 值可能为 -1，代表不可用或未定义；采集代码不能把它当正常容量参与计算。

![Java 17 容器内 JVM 内存结构](../assets/jvm-runtime-memory.svg)

解释 heap 之外的增长时继续看 [068](068-jvm-container-memory.md) 和 NMT，而不是反复调大 Xmx。

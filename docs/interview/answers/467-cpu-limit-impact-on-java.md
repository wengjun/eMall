# 467 CPU 配额如何影响 Java 17 的并行度？

[返回按分类学习面试题](../README.md)

## Java 侧先看这个值

```java
int processors = Runtime.getRuntime().availableProcessors();
System.out.println(processors);
```

HotSpot 的容器感知会影响这个可用处理器数量；它不是简单等于宿主机核数。
GC、JIT 和 ForkJoinPool 的默认策略可能参考处理器数量，具体行为还取决于 JDK 更新版本和显式参数。

## 两个不要混淆的控制

`-XX:ActiveProcessorCount=N` 可以覆盖 JVM 使用的处理器数量判断，
但**不会给容器增加 CPU 配额，也不会取消 OS throttling**。
把它设得很大可能只增加同时竞争配额的工作线程。

`ForkJoinPool.commonPool()` 的实际并行度要用 getParallelism 查看，
不要根据核数猜测，更不要据此让阻塞 IO 都使用公共池。

诊断时同时记录 JDK 发行版/更新号、JVM 看到的处理器数和容器配额。
GC 日志中 wall-clock 暂停变长不一定代表 GC 工作量同比增加，也可能是 GC 线程得不到 CPU。
具体 JVM 容器参数见 [068](068-jvm-container-memory.md)。

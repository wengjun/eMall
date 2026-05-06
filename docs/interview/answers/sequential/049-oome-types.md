# 049 `OutOfMemoryError` 常见类型有哪些？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

`OutOfMemoryError` 常见类型有哪些？

## 先给面试官的短答案

常见 OOM 包括 Java heap space、GC overhead limit exceeded、Metaspace、Direct buffer memory、
unable to create native thread，以及容器层面的 OOMKilled。

不同 OOM 的原因和排查方式不同，不能看到 OOM 就只加大 `-Xmx`。

## Java heap space

堆空间不足。

常见原因：

- 对象太多。
- 内存泄漏。
- 大查询一次性加载。
- 缓存无界。
- 队列积压。

排查：

- heap dump。
- Dominator Tree。
- GC 日志。

## GC overhead limit exceeded

JVM 花大量时间 GC，但回收效果很差。

通常说明堆接近耗尽，应用已经处于不可用边缘。

排查方向和堆 OOM 类似。

## Metaspace

元空间不足。

常见原因：

- 动态生成类太多。
- ClassLoader 泄漏。
- 热部署旧类无法卸载。
- 代理类无限生成。

排查：

- 类加载数量。
- ClassLoader 引用链。
- Metaspace 指标。

## Direct buffer memory

直接内存不足。

常见原因：

- NIO DirectByteBuffer 未释放。
- Netty 直接内存配置不合理。
- 文件或网络缓冲过大。
- 堆外缓存。

排查：

- direct memory 指标。
- NIO/Netty 配置。
- Native Memory Tracking。

## unable to create native thread

无法创建新线程。

常见原因：

- 线程数过多。
- 每个线程栈占用太大。
- OS 线程限制。
- 容器内存不足。

排查：

- 线程数。
- `jstack`。
- 线程池配置。
- OS ulimit。

## 容器 OOMKilled

Kubernetes 直接杀掉容器，不一定有 Java OOM 堆栈。

原因是进程总内存超过容器 limit。

总内存包括：

- Java 堆。
- 直接内存。
- 元空间。
- 线程栈。
- JVM native。
- 其他 native 库。

## 在 eMall 项目中怎么讲？

订单服务 OOM 可能是：

- 查询用户历史订单不分页。
- Outbox 一次加载太多事件。
- 本地缓存无限增长。

网关 OOM 可能是：

- 请求体缓存过大。
- 连接数过多。
- 线程或 Netty direct memory 问题。

## 深度增强：JVM OOM 类型图

![Java 17 容器内 JVM 内存结构](../../assets/jvm-runtime-memory.svg)

OOM 类型要和 JVM 内存区域对应。`Java heap space` 指向堆对象，`Metaspace` 指向类元数据，
`Direct buffer memory` 指向堆外缓冲，`unable to create native thread` 指向线程和 native 资源。
容器 `OOMKilled` 则表示整个进程超过 cgroup 限制，JVM 可能来不及抛异常。

## 深度增强：Java 17 OOM 分类代码示例

```java
enum OomCategory {
    HEAP,
    GC_OVERHEAD,
    METASPACE,
    DIRECT_MEMORY,
    NATIVE_THREAD,
    CONTAINER_OOM_KILLED,
    UNKNOWN
}

final class OomClassifier {

    OomCategory classify(String message, boolean containerKilled) {
        if (containerKilled) {
            return OomCategory.CONTAINER_OOM_KILLED;
        }
        if (message == null) {
            return OomCategory.UNKNOWN;
        }
        if (message.contains("Java heap space")) {
            return OomCategory.HEAP;
        }
        if (message.contains("GC overhead limit exceeded")) {
            return OomCategory.GC_OVERHEAD;
        }
        if (message.contains("Metaspace")) {
            return OomCategory.METASPACE;
        }
        if (message.contains("Direct buffer memory")) {
            return OomCategory.DIRECT_MEMORY;
        }
        if (message.contains("unable to create native thread")) {
            return OomCategory.NATIVE_THREAD;
        }
        return OomCategory.UNKNOWN;
    }
}
```

这段代码可以作为面试表达辅助：不同错误信息背后是不同资源耗尽，排查工具也不同。
堆看 heap dump，直接内存看 NMT 和 Netty 指标，线程看 `jstack` 和线程池，容器 OOM 看 Pod 事件。

## 深度增强：生产边界

不能把所有 OOM 都归结为内存太小。加大 `-Xmx` 可能掩盖泄漏，也可能挤压 direct memory 和线程栈，
导致容器更容易 OOMKilled。正确做法是先分类，再用对应证据定位。

对于 Kubernetes 服务，建议开启 OOM heap dump、保留 GC 日志、采集 container memory working set，
并记录 Pod termination reason。否则重启后现场丢失，事故复盘只能靠猜。

## 深度增强：面试高分表达

我会先问 OOM 是 JVM 抛出的，还是容器杀掉的。然后根据错误信息和内存区域分类：
堆、元空间、直接内存、线程和容器总内存。每类 OOM 都有不同证据和修复方式，
所以不能简单说“加内存”或“调大 Xmx”。

## 专家级完整回答

```text
常见 OOM 包括 Java heap space、GC overhead limit exceeded、Metaspace、
Direct buffer memory、unable to create native thread，以及 Kubernetes OOMKilled。
每种 OOM 的排查方向不同。

堆 OOM 看 heap dump 和对象引用；Metaspace 看类加载器和动态类；
Direct memory 看 NIO/Netty 和 MaxDirectMemorySize；native thread 看线程数、栈大小和 OS 限制；
容器 OOMKilled 要看整个进程内存，而不是只看 Xmx。
```

## 回答评分点

高分答案应该覆盖：

- 能列出多种 OOM。
- 能说明不同 OOM 原因不同。
- 能指出不能只加 Xmx。
- 能联系容器 OOMKilled。
- 能给出排查方向。
## 深度完善：专项验收清单

围绕「`OutOfMemoryError` 常见类型有哪些？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
回答时要把概念、代码、数据、失败路径和指标串起来，证明自己不是只理解单点知识。

### 项目落点

- 先说明它在 eMall 哪个模块或链路中出现，例如交易、库存、支付、搜索、风控、发布或可观测性。
- 再说明它保护的核心目标：正确性、可用性、延迟、成本、安全或协作效率。
- 最后补失败场景：超时、重试、重复请求、状态不一致、热点流量、配置错误或发布回滚。

### 验收证据

- 代码证据：关键类、状态机、唯一约束、事务边界、线程池隔离或配置项。
- 测试证据：单元测试、集成测试、契约测试、压测、故障注入或回归用例。
- 运行证据：指标看板、Trace、结构化日志、告警、Runbook、对账结果或补偿记录。

### 高分收束

面试最后要回到取舍：当前方案为什么足够简单可靠，什么时候需要升级，升级时如何灰度、回滚和验证。
这样回答能体现生产系统判断力，而不是只罗列技术名词。

深度完善标记：专题增强答案已补项目落点、验收证据和取舍收束。

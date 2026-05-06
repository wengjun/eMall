# 050 堆 OOM 和直接内存 OOM 如何区分？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

堆 OOM 和直接内存 OOM 如何区分？

## 先给面试官的短答案

堆 OOM 通常表现为 `java.lang.OutOfMemoryError: Java heap space`，问题在 Java 堆对象太多或无法回收；
直接内存 OOM 常见为 `OutOfMemoryError: Direct buffer memory`，问题在堆外直接内存不足。

排查时，堆 OOM 看 heap dump；直接内存 OOM 要看 NIO/Netty、DirectByteBuffer、Native Memory Tracking
和容器总内存。

## 堆 OOM

典型错误：

```text
java.lang.OutOfMemoryError: Java heap space
```

常见原因：

- 集合无限增长。
- 本地缓存无上限。
- 一次性加载大量数据。
- MQ 消费积压在内存。
- 对象引用未释放。

排查工具：

- heap dump。
- MAT。
- JProfiler。
- GC 日志。

关注：

- 哪类对象最多。
- 谁持有它们。
- Full GC 后是否回落。

## 直接内存 OOM

典型错误：

```text
java.lang.OutOfMemoryError: Direct buffer memory
```

常见原因：

- NIO `ByteBuffer.allocateDirect` 过多。
- Netty direct memory 使用过高。
- 文件上传下载缓冲。
- 网络连接过多。
- 直接内存限制太小。

排查工具：

- Native Memory Tracking。
- Netty metrics。
- JVM direct buffer metrics。
- 容器 memory usage。
- `jcmd VM.native_memory`。

## 如何快速区分？

### 看错误信息

`Java heap space` 通常是堆。

`Direct buffer memory` 通常是直接内存。

### 看 heap used

如果 Pod 内存很高，但 heap used 不高，可能是堆外内存、线程栈、元空间或 native 内存。

### 看 heap dump

堆 OOM 的主要对象能在 heap dump 中看到。
直接内存 OOM 的堆 dump 可能看不到真正占用的大块 native memory，只能看到 DirectByteBuffer 引用。

## 容器环境中的坑

容器 memory limit 包含整个进程内存。

如果：

```text
memory limit = 1 GB
Xmx = 900 MB
```

再加上直接内存、线程栈、元空间，容器可能被 OOMKilled。

所以要留出堆外余量。

## 在 eMall 项目中怎么讲？

网关或高性能 HTTP 客户端更可能遇到直接内存问题，因为它们处理大量网络缓冲。
订单服务如果因为缓存或查询对象太多，更可能是堆 OOM。

如果使用 WebFlux、Netty、Kafka 客户端、OpenSearch 客户端，都要关注堆外内存。

## 深度增强：堆和直接内存图

![Java 17 容器内 JVM 内存结构](../../assets/jvm-runtime-memory.svg)

堆 OOM 和直接内存 OOM 的核心区别是“对象可见性”。堆对象完整存在于 heap dump 中；
直接内存真正的大块内存在 native 区域，heap dump 通常只能看到引用它的 `DirectByteBuffer` 对象。

## 深度增强：Java 17 直接内存风险示例

```java
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

final class DirectBufferPressure {
    private final List<ByteBuffer> retainedBuffers = new ArrayList<>();

    void retainOneMegabyteBuffer() {
        retainedBuffers.add(ByteBuffer.allocateDirect(1024 * 1024));
    }

    int retainedCount() {
        return retainedBuffers.size();
    }
}
```

这段代码会持续保留 direct buffer 引用。即使 heap 中只看到一批 `DirectByteBuffer` 小对象，
它们背后可能挂着大量 native memory。Netty、网关、上传下载和高性能 HTTP client 都要特别关注。

## 深度增强：生产判断方法

如果 `heap used` 不高，但容器 memory working set 很高，要怀疑 direct memory、线程栈、metaspace
或其他 native memory。此时不要只分析 heap dump，还要看 NMT、direct buffer metrics、线程数和 Pod 事件。

容器内 `-Xmx` 要给堆外留余量。网关类服务 direct memory 占比可能比普通业务服务更高；
订单服务可能更多是堆对象和缓存压力。不同服务不能使用完全相同的内存比例。

## 深度增强：面试高分表达

我会先看错误信息，再看 heap used 和容器总内存。如果是 `Java heap space`，重点分析 heap dump；
如果是 `Direct buffer memory` 或 heap 不高但 Pod 内存很高，重点看 NIO、Netty、NMT 和 direct buffer。
这说明我能区分 JVM 内部堆问题和容器进程级内存问题。

## 专家级完整回答

```text
堆 OOM 通常是 Java heap space，说明堆对象过多或无法回收，排查重点是 heap dump、
Dominator Tree 和 GC Roots 引用链。直接内存 OOM 通常是 Direct buffer memory，
说明 NIO/Netty 等堆外内存不足，heap dump 可能看不出完整占用，需要看 Native Memory Tracking、
direct buffer 指标和容器总内存。

在 Kubernetes 中，Pod memory limit 包含堆、直接内存、线程栈、元空间和 JVM native 开销，
所以不能把 Xmx 设置得接近容器上限。
```

## 回答评分点

高分答案应该覆盖：

- 能区分错误信息。
- 堆 OOM 看 heap dump。
- 直接内存 OOM 看 NIO/Netty/NMT。
- 能指出 heap used 不高也可能容器 OOM。
- 能结合网关和网络客户端场景。

# 068 容器环境下 JVM 如何感知内存限制？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

容器环境下 JVM 如何感知内存限制？

## 先给面试官的短答案

现代 JVM 能读取 cgroup 信息，感知容器的 CPU 和内存限制。Java 17 默认支持容器感知，
可以根据容器 memory limit 计算默认堆大小，也可以通过 `MaxRAMPercentage` 控制堆占比。

但 JVM 感知容器限制不代表自动安全。Pod 内存还包括堆外内存、线程栈、元空间和 JVM native 开销。

## 为什么容器内存特殊？

传统物理机上，JVM 看到的是机器总内存。

容器中，应用应该受到 Pod 或 container memory limit 限制。

如果 JVM 不感知容器限制，可能按宿主机内存计算堆大小，导致容器内存超限。

现代 JVM 已经解决了这个基础问题，但仍需要合理配置。

## cgroup 是什么？

cgroup 是 Linux 用来限制和统计进程资源的机制。

Kubernetes 的 CPU 和内存限制最终会通过 cgroup 作用到容器进程。

JVM 会读取 cgroup 信息来判断：

- 可用内存限制。
- CPU 配额。
- CPU 核数。

Java 17 对容器环境支持已经比较成熟。

## JVM 如何设置堆比例？

可以使用：

```text
-XX:MaxRAMPercentage=65
-XX:InitialRAMPercentage=65
```

含义是让堆按可用 RAM 的百分比设置。

例如容器内存 2 GB，`MaxRAMPercentage=65`，最大堆大约是 1.3 GB。

剩余内存留给 metaspace、direct memory、线程栈、code cache 和 native 开销。

## 为什么不能把堆设满？

容器 memory limit 统计整个进程。

除了 heap，还有：

- direct memory。
- metaspace。
- thread stack。
- code cache。
- GC native memory。
- JIT 编译开销。
- libc 和 TLS 等 native 开销。

如果 `-Xmx` 接近容器上限，heap 还没满，容器也可能 OOMKilled。

## CPU 感知也很重要

JVM 还会根据 CPU 配额调整：

- GC 线程数量。
- JIT 编译线程数量。
- ForkJoinPool 并行度。

如果 CPU limit 设置过小，服务可能出现：

- 启动变慢。
- GC 变慢。
- JIT 编译慢。
- P99 升高。
- CPU throttling。

所以容器中不只要看内存，还要看 CPU limit 和 request。

## 如何确认 JVM 看到的限制？

可以使用：

```powershell
java -XshowSettings:system -version
```

也可以查看：

```powershell
jcmd <pid> VM.flags
jcmd <pid> VM.info
```

线上还应结合容器指标：

- container memory working set。
- heap used。
- non-heap used。
- direct memory。
- thread count。
- CPU throttling。

## OOMKilled 和 Java OOM 的区别

Java OOM 是 JVM 抛出的异常，例如：

```text
java.lang.OutOfMemoryError: Java heap space
```

OOMKilled 是容器被操作系统杀掉，Java 进程可能没有机会打印 heap dump。

如果 Pod 直接重启且日志中没有 Java OOM，要怀疑容器级 OOMKilled。

## 在 eMall 项目中怎么讲？

eMall 网关处理大量网络请求，direct memory 和连接缓冲占用较高。

如果容器 1 GB 内存，不能把 `-Xmx` 设置为 900 MB，因为网关还需要堆外内存和线程栈。

订单服务可能 heap 占比更高，但也要给连接池、线程和元空间留余量。

不同模块应该按资源模型设置 JVM 参数。

## 深度增强：容器内 JVM 内存图

![Java 17 容器内 JVM 内存结构](../assets/jvm-runtime-memory.svg)

Java 17 能读 cgroup 限制，只解决“JVM 知道容器有多大”这个问题。它不能自动判断业务需要多少 direct memory、
多少线程栈、多少 metaspace，也不能替你避免线程池和缓存把内存吃满。

## 深度增强：Java 17 内存预算代码示例

```java
record ContainerMemoryPlan(
        long limitMb,
        long heapMb,
        long directMb,
        long metaspaceMb,
        long threadStackMb,
        long reservedMb) {

    boolean fits() {
        long total = heapMb + directMb + metaspaceMb + threadStackMb + reservedMb;
        return total <= limitMb;
    }

    int heapPercentage() {
        return Math.toIntExact(heapMb * 100 / limitMb);
    }
}
```

这段代码体现容器 JVM 的核心思路：先做总预算，再设 heap 比例。不能只说容器 2 GB，
就把 `-Xmx` 设成 1800 MB。剩余空间不够时，heap 没满也可能被 OOMKilled。

## 深度增强：生产边界

容器 CPU limit 也会影响 JVM。CPU 被 throttle 时，GC 线程、JIT 编译和业务线程都会变慢，
表现可能是 P99 升高而不是 CPU 使用率 100%。所以容器排障要看 `throttled_seconds` 和 CPU request/limit。

要区分 Java OOM 和 OOMKilled。Java OOM 通常有异常和 dump；OOMKilled 可能只有 Pod event 和退出码。
如果日志中没有 `OutOfMemoryError`，但 Pod reason 是 OOMKilled，就要看进程总内存，而不是只看 heap。

## 深度增强：面试高分表达

我会说 Java 17 已经支持容器感知，但这只是基础能力。生产上要用 `MaxRAMPercentage` 或 `-Xmx`
控制 heap，并为 direct memory、metaspace、线程栈和 native 开销留余量。排查时同时看 JVM 指标和
container memory，避免把容器 OOMKilled 误判成单纯 heap OOM。

## 专家级完整回答

```text
Java 17 可以通过 cgroup 感知容器 CPU 和内存限制，并根据容器 memory limit 计算默认堆大小。
生产中我会用 MaxRAMPercentage 或明确的 Xmx 控制堆占比，但不会把堆设置到接近容器上限，
因为 Pod 内存还包括 direct memory、metaspace、thread stack、code cache 和 JVM native 开销。

排查时要区分 Java OOM 和容器 OOMKilled。前者通常有 JVM 异常和 dump，后者可能是进程直接被杀。
```

## 回答评分点

高分答案应该覆盖：

- Java 17 支持 cgroup 容器感知。
- `MaxRAMPercentage` 控制堆占比。
- 容器内存不只包含 heap。
- CPU limit 会影响 GC/JIT/延迟。
- 区分 Java OOM 和 OOMKilled。
## 深度完善：专项验收清单

围绕「容器环境下 JVM 如何感知内存限制？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

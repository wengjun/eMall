# 053 jmap、jcmd、JFR 分别适合什么场景？

[返回按分类学习面试题](../README.md)

## 题目

`jmap`、`jcmd`、JFR 分别适合什么场景？

## 先给面试官的短答案

`jmap` 更偏传统内存排查，常用于生成 heap dump 和查看堆对象概况。
`jcmd` 是更通用的 JVM 诊断入口，可以触发 GC、查看 JVM 参数、生成 dump、查看 native memory、
启动 JFR。JFR 是低开销持续事件记录工具，适合生产性能分析、延迟分析、锁分析和 GC 分析。

现在生产环境我会优先考虑 `jcmd` 和 JFR，`jmap` 作为兼容和补充工具。

## 三者定位

可以这样理解：

- `jmap`：主要看 Java 堆。
- `jcmd`：统一 JVM 诊断命令入口。
- JFR：持续记录 JVM 和应用运行事件。

如果只是想看某一刻堆里有什么，`jmap` 和 `jcmd GC.heap_dump` 都可以。
如果想知道一段时间内为什么慢，JFR 更合适。

## jmap 适合什么场景？

`jmap` 常见用途：

- 导出 heap dump。
- 查看堆对象直方图。
- 分析堆 OOM。
- 分析大对象和对象数量异常。
- 配合 MAT 查找内存泄漏。

示例：

```powershell
jmap -histo <pid>
jmap -dump:format=b,file=heap.hprof <pid>
```

使用 heap dump 时要注意文件很大，生产机器磁盘可能被打满。

## jmap 的风险

一些 `jmap` 操作可能造成 Stop-The-World，或者对目标 JVM 有明显影响。

风险包括：

- dump 文件很大。
- 导出过程消耗 IO。
- 可能拉长服务停顿。
- 在高峰期影响线上请求。

所以生产上不要随便在高峰期 dump heap。更安全的方式是：

- 在副本实例上操作。
- 先确认磁盘空间。
- 在流量低峰执行。
- 优先使用 JFR 或指标做初步判断。

## jcmd 适合什么场景？

`jcmd` 是 JDK 自带的通用诊断工具。

常见用途：

- 查看 JVM 启动参数。
- 查看系统属性。
- 导出 heap dump。
- 查看类加载统计。
- 查看线程信息。
- 查看 native memory。
- 启动和停止 JFR。
- 触发诊断命令。

示例：

```powershell
jcmd <pid> VM.flags
jcmd <pid> VM.command_line
jcmd <pid> GC.class_histogram
jcmd <pid> GC.heap_dump heap.hprof
jcmd <pid> VM.native_memory summary
```

`jcmd` 的覆盖面比 `jmap` 更广，很多新诊断能力都集中在 `jcmd`。

## Native Memory Tracking

如果怀疑堆外内存问题，`jcmd` 很重要。

示例：

```powershell
jcmd <pid> VM.native_memory summary
```

它能帮助分析：

- Java heap。
- class metadata。
- thread stack。
- code cache。
- GC native memory。
- internal native memory。
- direct buffer 相关占用。

前提是 JVM 启动时启用了 NMT，例如：

```text
-XX:NativeMemoryTracking=summary
```

## JFR 适合什么场景？

JFR 是 Java Flight Recorder。

它会记录一段时间内的 JVM 和应用事件，适合分析：

- CPU 热点。
- 方法耗时。
- GC 暂停。
- 对象分配。
- 锁竞争。
- 线程阻塞。
- 文件 IO。
- Socket IO。
- 异常频率。
- 类加载。

JFR 的优势是低开销、时间维度完整，适合生产环境长期或按需开启。

## JFR 示例

可以通过 `jcmd` 启动 JFR：

```powershell
jcmd <pid> JFR.start name=profile settings=profile duration=120s filename=profile.jfr
```

然后用 JDK Mission Control 分析。

JFR 比单次 `jstack` 或 heap dump 更适合回答“这两分钟到底发生了什么”。

## 如何选择？

可以按问题类型选择：

- 堆对象异常：`jmap`、`jcmd GC.class_histogram`、heap dump。
- native memory 异常：`jcmd VM.native_memory`。
- CPU 飙高：JFR、async-profiler、`jstack`。
- P99 升高：JFR、链路追踪、线程 dump、依赖监控。
- GC 抖动：JFR、GC log、JVM metrics。
- 锁竞争：JFR、`jstack`。

生产排查通常不是单一工具，而是多工具交叉验证。

## 在 eMall 项目中怎么讲？

如果 eMall 网关内存持续上涨但 heap used 不高，优先用 `jcmd VM.native_memory` 看 direct memory、
thread stack 和 metaspace。

如果订单服务疑似 Java heap 泄漏，导出 heap dump 后用 MAT 查引用链。

如果秒杀接口 P99 飙升但没有明显错误，使用 JFR 记录高峰 2 分钟，看锁竞争、对象分配、GC 和 socket IO。

## 深度增强：JVM 生产运行图

![Java 17 容器内 JVM 内存结构](../assets/jvm-runtime-memory.svg)

JVM 题要从运行时资源解释到业务影响。堆、直接内存、元空间、线程栈和容器 memory limit 共同决定服务稳定性；
GC、CPU throttling、线程池队列和下游超时会一起影响 P99，而不是孤立存在。

## 深度增强：Java 17 诊断模型示例

```java
record RuntimeSignal(
        double heapUsage,
        double containerMemoryUsage,
        long gcPauseMillis,
        int threadCount,
        int queuedTasks) {

    boolean requiresTriage() {
        return heapUsage > 0.85
                || containerMemoryUsage > 0.90
                || gcPauseMillis > 500
                || threadCount > 800
                || queuedTasks > 1_000;
    }
}
```

这个模型强调线上诊断要看组合信号。只看 heap 不够，只看 GC 也不够；
要把 JVM、容器、线程池和业务延迟放到同一条时间线。

## 深度增强：生产边界

JVM 调优不能靠背参数。要先明确服务目标：低延迟、吞吐、容器资源、对象分配速率和 P99 SLO。
然后通过 GC 日志、JFR、指标和压测验证。错误地调大 `-Xmx` 可能挤压堆外内存，导致容器 OOMKilled。

## 深度增强：面试高分表达

我会用证据链回答 JVM 问题：先看业务影响，再看 JVM 指标、GC 日志、线程栈、heap dump、容器事件和最近变更。
结论要能解释现象，并能给出降级、扩容、参数调整或代码优化方案。

## 专家级完整回答

```text
jmap 更偏堆内存诊断，适合对象直方图和 heap dump。jcmd 是更现代的统一诊断入口，
可以看 JVM 参数、类加载、heap dump、native memory，也能控制 JFR。JFR 是低开销事件记录，
适合生产性能分析，尤其是 CPU、锁、GC、分配和 IO 的时间序列分析。

我的原则是：先用指标和 JFR 做低风险定位，再在必要时对特定实例做 heap dump。生产环境要考虑
STW、磁盘空间和对线上流量的影响。
```

## 回答评分点

高分答案应该覆盖：

- `jmap` 主要面向堆。
- `jcmd` 是统一诊断入口。
- JFR 适合生产低开销性能分析。
- 知道 heap dump 有风险。
- 能结合 native memory、GC、CPU、P99 场景选工具。

## 深度完善：面向 L6 的回答框架

围绕「jmap、jcmd、JFR 分别适合什么场景？」，高分答案不能停在概念定义，而要把「内存模型、GC、线程、JIT、诊断命令和容器资源边界」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「JVM 和性能诊断」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `gateway、order、payment、search 在高峰期的 P99、GC、线程池和容器资源` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：P50/P95/P99、GC pause、allocation rate、线程数、CPU throttle、RSS。
- 验证证据：JFR、GC log、jstack、jcmd、heap dump、压测报告和发布对比曲线。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引
本题复习重点：`jmap`、`jcmd`、JFR 分别适合什么场景？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

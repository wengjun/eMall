# 072 需要重点监控哪些 JVM 指标？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

需要重点监控哪些 JVM 指标？

## 先给面试官的短答案

重点监控 heap、old gen、metaspace、direct memory、GC 次数和暂停、allocation rate、线程数、
线程状态、类加载数量、CPU、进程内存、容器内存和线程池指标。对微服务来说，还要把 JVM 指标和接口 P99、
错误率、QPS、连接池、下游延迟一起看。

最重要的是 old gen 趋势、GC pause、线程堆积和容器内存余量。

## 内存指标

需要关注：

- heap used。
- heap committed。
- old gen used。
- young gen used。
- metaspace used。
- non-heap used。
- direct buffer memory。
- mapped buffer memory。

old gen 持续上涨且 GC 后不回落，通常比 heap 瞬时高更危险。

## GC 指标

需要关注：

- young GC count。
- young GC duration。
- old GC count。
- old GC duration。
- Full GC count。
- GC pause max。
- GC pause P99。
- GC CPU 占比。
- allocation rate。
- promotion rate。

GC 告警要关注持续恶化和与 P99 的相关性。

## 线程指标

需要关注：

- live thread count。
- daemon thread count。
- peak thread count。
- runnable 线程数。
- blocked 线程数。
- waiting 线程数。
- deadlock 检测。

线程数持续上涨可能表示线程泄漏、线程池不受控或下游阻塞。

## 类加载指标

需要关注：

- loaded class count。
- total loaded class count。
- unloaded class count。

类加载数量异常上涨可能来自动态代理、脚本、插件、表达式编译或类加载器泄漏。

普通业务服务类加载数量通常启动后趋于稳定。

## 进程和容器指标

需要关注：

- process CPU。
- system CPU。
- process RSS。
- container memory working set。
- container memory limit。
- CPU throttling。
- file descriptor count。

容器 OOMKilled 很多时候不是 heap OOM，而是进程总内存超过限制。

## 线程池和连接池指标

严格说它们不是 JVM 原生指标，但对 Java 服务非常关键。

线程池：

- active count。
- pool size。
- queue size。
- completed task count。
- rejected count。
- task execution time。

连接池：

- active connections。
- idle connections。
- pending acquire。
- acquire latency。
- timeout count。

线程池和连接池经常是 P99 升高的直接原因。

## 指标优先级

如果只能先做一批告警，建议优先：

- old gen 使用率和回收趋势。
- GC pause P99。
- Full GC 次数。
- live thread count。
- blocked thread count。
- container memory 使用率。
- 业务线程池 queue size。
- HTTP/DB 连接池 pending。

这些指标最容易提前发现线上风险。

## 在 eMall 项目中怎么讲？

订单服务重点看 old gen、GC pause、订单线程池、数据库连接池和下游 HTTP 连接池。

网关重点看 direct memory、线程数、event loop 延迟、连接数和容器内存。

营销服务重点看规则计算 CPU、对象分配速率和缓存大小。

不同服务的 JVM 监控重点应该按服务特性调整。

## 深度增强：JVM 生产运行图

![Java 17 容器内 JVM 内存结构](../../assets/jvm-runtime-memory.svg)

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
核心 JVM 指标包括 heap/old gen/metaspace/direct memory、GC 次数和暂停、allocation rate、
线程数和线程状态、类加载数量、CPU、进程内存和容器内存。生产上还必须补充线程池、数据库连接池、
HTTP 连接池指标，因为很多尾延迟问题来自资源池排队。

我最关注 old gen 是否 GC 后回落、GC pause 是否影响 P99、线程数是否持续上涨、容器内存是否接近 limit。
```

## 回答评分点

高分答案应该覆盖：

- 内存、GC、线程、类加载、CPU、容器都要看。
- old gen 趋势比瞬时 heap 更重要。
- direct memory 和容器内存不能漏。
- 线程池和连接池也要监控。
- 能按服务类型区分监控重点。

## 深度完善：面向 L6 的回答框架

围绕「需要重点监控哪些 JVM 指标？」，高分答案不能停在概念定义，而要把「内存模型、GC、线程、JIT、诊断命令和容器资源边界」讲成一条可验证的工程链路。
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

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：需要重点监控哪些 JVM 指标？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

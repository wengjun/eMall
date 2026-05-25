# 070 线上是否应该主动调用 System.gc()？

[返回按分类学习面试题](../README.md)

## 题目

线上是否应该主动调用 `System.gc()`？

## 先给面试官的短答案

线上业务代码不应该主动调用 `System.gc()`。它只是向 JVM 建议执行 GC，具体是否执行由 JVM 决定；
一旦触发 Full GC 或全局停顿，可能导致 P99 抖动。生产中应该通过合理内存配置、对象生命周期治理和 GC 策略解决问题，
而不是靠手动 GC。

如果第三方库调用了 `System.gc()`，可以评估使用 `-XX:+DisableExplicitGC`。

## System.gc() 做什么？

`System.gc()` 的语义是建议 JVM 尽力执行垃圾回收。

它不是强制命令。

但在很多 JVM 配置下，显式 GC 可能触发比较重的 GC 行为。

对低延迟服务来说，这种不可控停顿很危险。

## 为什么业务代码不该调用？

主要原因：

- 破坏 JVM 自适应 GC 策略。
- 可能触发 Full GC。
- 造成 Stop-The-World 暂停。
- 抬高 P99 和 P999。
- 在高峰期放大延迟抖动。
- 容易掩盖真正内存问题。

GC 应该由 JVM 根据堆使用、分配速率和 GC 策略决定，而不是由业务代码随意触发。

## 常见错误场景

一些代码会在这些地方调用：

- 大批量任务结束后。
- 文件导出结束后。
- 图片处理结束后。
- 缓存清理后。
- 单元测试复制到生产代码。
- 第三方库释放 direct buffer。

这些看起来像“主动释放内存”，但 JVM 不一定需要立刻 GC。

更好的方式是减少对象持有、分批处理和及时关闭资源。

## DisableExplicitGC

可以使用：

```text
-XX:+DisableExplicitGC
```

它会让显式 GC 调用失效或被忽略。

适合场景：

- 第三方库频繁调用 `System.gc()`。
- 低延迟服务要避免显式 Full GC。
- 无法快速修改代码来源。

但启用前要验证是否影响某些依赖的资源释放逻辑，尤其是直接内存相关场景。

## DirectByteBuffer 的历史问题

一些历史代码会依赖 `System.gc()` 触发 direct buffer 清理。

这种方式不可靠。

更好的做法：

- 控制 direct memory 上限。
- 使用成熟网络框架管理 buffer。
- 监控 direct memory。
- 复用 buffer。
- 避免无限创建直接缓冲。

不要把堆外内存治理寄托在显式 GC 上。

## 如果内存真的很高怎么办？

正确做法是排查：

- 是否有内存泄漏。
- 缓存是否无上限。
- 队列是否堆积。
- 是否一次性加载大数据。
- 是否存在大对象。
- 是否有 direct memory 泄漏。
- GC 参数是否合理。

如果只是调用 `System.gc()` 后内存下降，说明对象确实可回收，但不代表设计合理。

要找为什么这些对象会集中产生，是否可以分批、限流或缩短生命周期。

## 在 eMall 项目中怎么讲？

例如对账导出任务结束后，不应该直接调用 `System.gc()`。

更好的做法是：

- 分页读取订单。
- 流式写文件。
- 限制导出并发。
- 及时关闭流。
- 对大任务使用独立 worker。
- 监控 heap 和 GC。

核心交易服务更不能在请求链路里调用显式 GC，否则可能影响所有用户请求。

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
线上业务代码不应该主动调用 System.gc()。它只是建议 JVM 执行 GC，具体行为取决于 JVM，
但可能触发 Full GC 或明显停顿，导致 P99/P999 抖动。内存问题应该通过对象生命周期治理、
缓存上限、分批处理、GC 参数和泄漏排查解决，而不是用手动 GC 掩盖。

如果第三方库频繁显式 GC，可以评估 -XX:+DisableExplicitGC，但要验证 direct memory 等场景是否受影响。
```

## 回答评分点

高分答案应该覆盖：

- `System.gc()` 不是强制命令。
- 显式 GC 可能导致 Full GC 或停顿。
- 业务代码不应调用。
- 可以评估 `DisableExplicitGC`。
- 内存高要查对象生命周期和泄漏。

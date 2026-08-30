# 467 CPU limit 对 Java 服务有什么影响？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

CPU limit 会让容器超过配额后被 throttling。对 Java 服务来说，这会导致请求延迟升高、GC 变慢、
线程调度变慢、P99 抖动和吞吐下降。

CPU 使用率看起来不一定满，但 throttling 已经在影响延迟。

## throttling

CPU limit 通过 CFS 配额控制。

当容器在一个周期内用完 CPU 配额后，会被暂停到下个周期。暂停期间应用线程无法继续执行。

## 对 Java 的影响

影响：

- 业务线程执行变慢。
- GC 线程执行变慢。
- ForkJoinPool 并发度受影响。
- 定时任务延迟。
- P99 明显升高。
- 超时和重试增加。

尾延迟经常先暴露问题。

## 排查指标

看：

- CPU throttled time。
- CPU throttled periods。
- P99 延迟。
- GC pause。
- 线程池队列。
- 容器 CPU usage。

不能只看 CPU 平均使用率。

## 电商系统实践

大型电商系统订单服务在高峰期 P99 抖动，如果 CPU throttling 明显，即使平均 CPU 只有 60%，也可能是
CPU limit 太紧造成。

可以提高 limit、优化线程池、降低同步计算或调整 HPA 指标。

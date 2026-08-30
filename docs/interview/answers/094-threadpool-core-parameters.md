# 094 线程池核心参数如何设置？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

线程池核心参数包括 corePoolSize、maximumPoolSize、keepAliveTime、workQueue、threadFactory 和 rejectedExecutionHandler。
设置时要基于任务类型、平均耗时、目标 QPS、CPU 核数、下游容量和可接受排队时间。生产重点是有界队列、
明确拒绝策略、线程命名和指标监控。

线程池参数不是拍脑袋，也不是越大越好。

## 核心参数

`ThreadPoolExecutor` 主要参数：

- corePoolSize：核心线程数。
- maximumPoolSize：最大线程数。
- keepAliveTime：非核心线程空闲存活时间。
- workQueue：任务队列。
- threadFactory：线程创建工厂。
- rejectedExecutionHandler：拒绝策略。

每个参数都影响过载行为。

## corePoolSize

核心线程数决定常态并发能力。

CPU 密集任务通常接近 CPU 核数。

IO 密集任务可以更大，因为线程会等待外部 IO。

但线程数过大也会增加上下文切换和内存占用。

## maximumPoolSize

最大线程数决定突发流量时最多能扩到多少。

如果队列是无界队列，maximumPoolSize 可能基本不起作用，因为任务一直进队列。

所以要理解队列和最大线程数的配合。

## workQueue

队列是最关键参数之一。

无界队列会把压力变成内存堆积。

生产通常使用有界队列，并根据可接受排队时间设置容量。

队列越大，不代表系统越稳，可能只是更晚失败。

## rejectedExecutionHandler

拒绝策略决定过载时如何保护系统。

常见策略：

- AbortPolicy：抛异常。
- CallerRunsPolicy：调用方线程执行。
- DiscardPolicy：直接丢弃。
- DiscardOldestPolicy：丢弃最老任务。
- 自定义策略：记录指标、返回降级。

核心链路通常要自定义拒绝处理，不能静默丢任务。

## threadFactory

线程名非常重要。

应该给线程池设置清晰线程名，例如：

```text
order-create-worker-1
payment-query-worker-1
```

这样 `jstack` 和日志排查时能快速定位业务。

## 监控指标

必须监控：

- active count。
- pool size。
- queue size。
- rejected count。
- completed task count。
- task wait time。
- task execution time。

没有监控的线程池无法生产治理。

## 电商系统实践

订单创建线程池要根据下单 QPS、库存和支付下游容量、订单处理耗时设置。

如果库存下游最多支持单实例 200 并发，订单服务线程池不能无限放大库存调用。

否则上游扩容会把下游打垮。

# 094 线程池核心参数如何设置？

[返回按分类学习面试题](../README.md)

## 题目

线程池核心参数如何设置？

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

## 在 eMall 项目中怎么讲？

订单创建线程池要根据下单 QPS、库存和支付下游容量、订单处理耗时设置。

如果库存下游最多支持单实例 200 并发，订单服务线程池不能无限放大库存调用。

否则上游扩容会把下游打垮。

## 深度增强：并发治理图

![Java 并发从线程安全到容量保护](../assets/concurrency-governance.svg)

并发题不能只回答 API 用法。生产系统要同时考虑线程安全、资源隔离、超时、拒绝、幂等和分布式多实例。
单机锁只能保护当前 JVM，不能保护整个集群；线程池满也不是小问题，而是容量和可用性风险。

## 深度增强：Java 17 有界并发示例

```java
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

final class BulkheadGuard {
    private final Semaphore permits;

    BulkheadGuard(int maxConcurrentCalls) {
        this.permits = new Semaphore(maxConcurrentCalls);
    }

    <T> T execute(Supplier<T> supplier) {
        if (!permits.tryAcquire()) {
            throw new IllegalStateException("Bulkhead rejected the call");
        }
        try {
            return supplier.get();
        } finally {
            permits.release();
        }
    }
}
```

这段代码展示了并发控制的生产思路：不是让所有请求无限进入系统，而是在入口保护共享资源。
真实项目还要加超时、指标、降级和按下游隔离。

## 深度增强：生产边界

线程安全不等于系统安全。`ConcurrentHashMap` 只能保护当前进程内的数据结构，
不能替代数据库唯一键、幂等表或分布式一致性。线程池也不能使用无界队列，
否则会把过载转化成内存上涨和 P99 恶化。

## 深度增强：面试高分表达

我会把并发问题分成三层：JMM 和锁保证单机正确性，线程池和隔离保证资源不被拖垮，
幂等和唯一键保证分布式正确性。这样能体现我理解 Java 并发，也理解微服务生产稳定性。

## 专家级完整回答

```text
线程池参数要从任务类型、QPS、任务耗时、CPU 核数、下游容量和可接受排队时间出发。
核心线程数决定常态能力，最大线程数处理突发，队列必须有界，拒绝策略要符合业务语义，
线程名和指标必须完善。

我不会用无界队列，也不会只靠扩大线程数解决慢问题。线程池的目标是保护系统在过载时可控失败。
```

## 回答评分点

高分答案应该覆盖：

- 六个核心参数。
- 队列必须重点设计。
- 有界队列和拒绝策略。
- 线程命名和监控。
- 参数要结合下游容量。

## 深度完善：面向 L6 的回答框架

围绕「线程池核心参数如何设置？」，高分答案不能停在概念定义，而要把「线程安全、锁、CAS、线程池、隔离、超时和并发容量」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「并发和线程治理」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `order 创建、inventory 扣减、payment 回调、outbox relay 和异步补偿任务` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：线程池活跃数、队列长度、拒绝数、锁等待、超时率、重复请求数。
- 验证证据：并发单测、压测曲线、线程 dump、拒绝日志、幂等记录和容量评估。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引
本题复习重点：线程池核心参数如何设置？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

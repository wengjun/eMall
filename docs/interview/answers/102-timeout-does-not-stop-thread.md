# 102 任务超时后线程是否真的停止？

[返回按分类学习面试题](../README.md)

## 题目

任务超时后线程是否真的停止？

## 先给面试官的短答案

不一定。很多超时只是调用方不再等待结果，底层任务线程可能仍在运行。`Future.get(timeout)` 超时不会自动杀死线程；
`CompletableFuture.orTimeout` 也主要是让 future 超时完成。要真正停止任务，需要任务支持取消、中断、超时 IO 或协作式退出。

生产中要区分“调用方超时”和“任务真正停止”。

## 调用方超时

示例：

```java
future.get(100, TimeUnit.MILLISECONDS);
```

如果 100 ms 未完成，调用方得到 `TimeoutException`。

但执行任务的线程可能还在继续跑。

这意味着资源仍然被占用。

## cancel(true)

可以调用：

```java
future.cancel(true);
```

`true` 表示尝试中断执行线程。

注意是“尝试”。

如果任务不响应中断，仍然不会停。

## 中断不是强杀

Java 中断是协作机制。

线程需要主动检查：

```java
Thread.currentThread().isInterrupted()
```

或者调用可中断阻塞方法时抛出 `InterruptedException`。

如果代码是死循环且不检查中断，中断无法让它停止。

## IO 超时很重要

下游 HTTP 或数据库调用必须设置底层超时。

否则即使上层 future 超时，底层 socket 可能仍然阻塞。

需要设置：

- connect timeout。
- read timeout。
- request timeout。
- connection acquire timeout。

超时必须贯穿调用链。

## 为什么这很危险？

如果请求层超时返回，但任务仍在后台运行：

- 线程继续被占用。
- 下游继续被调用。
- 数据库连接继续占用。
- 用户重试会叠加新任务。
- 可能造成重复写入。

这会放大雪崩风险。

## 在 eMall 项目中怎么讲？

订单创建调用库存超时后，不能只让前端超时返回。

还要确认：

- 库存 HTTP 客户端是否有 read timeout。
- 后台任务是否可取消。
- 订单操作是否幂等。
- 下游是否会继续扣库存。
- 超时后是否有补偿和状态确认。

否则可能出现用户看到失败，但库存稍后被扣减。

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
任务超时不等于线程停止。Future.get(timeout) 只是调用方不再等待，CompletableFuture.orTimeout
也主要是让 future 超时完成，底层任务可能仍在运行。要真正停止，需要 cancel(true)、任务响应中断、
阻塞 IO 设置底层超时，并设计幂等和补偿。

生产排查超时问题时，我会确认调用方超时、线程释放、连接释放和下游操作是否都正确结束。
```

## 回答评分点

高分答案应该覆盖：

- 调用方超时不等于任务停止。
- `cancel(true)` 只是尝试中断。
- 中断是协作机制。
- IO 必须设置底层超时。
- 要考虑幂等和补偿。

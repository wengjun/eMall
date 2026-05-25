# 090 CountDownLatch、CyclicBarrier、Semaphore 分别适合什么场景？

[返回按分类学习面试题](../README.md)

## 题目

`CountDownLatch`、`CyclicBarrier`、`Semaphore` 分别适合什么场景？

## 先给面试官的短答案

`CountDownLatch` 适合一个线程等待多个任务完成，一次性使用；`CyclicBarrier` 适合一组线程互相等待，
到齐后一起继续，并且可以循环使用；`Semaphore` 适合控制并发许可数量，例如限制同时访问某个资源的线程数。

三者都是协调线程，不是替代业务限流和分布式协调的万能工具。

## CountDownLatch

`CountDownLatch` 是倒计时门闩。

初始化一个计数，任务完成后 `countDown()`，等待线程调用 `await()`。

示例：

```java
CountDownLatch latch = new CountDownLatch(3);

executor.submit(() -> {
    loadUser();
    latch.countDown();
});

latch.await();
```

适合：

- 主线程等待多个子任务完成。
- 并发测试同时发起请求。
- 启动时等待多个组件初始化。

特点是一次性使用，计数归零后不能重置。

## CyclicBarrier

`CyclicBarrier` 是循环屏障。

多个线程都调用 `await()`，等到指定数量线程都到达后，一起继续执行。

适合：

- 多线程分阶段计算。
- 所有参与者到齐后进入下一轮。
- 并行任务每轮同步。

它可以循环使用。

如果某个线程失败，屏障可能被破坏，其他线程会收到异常。

## Semaphore

`Semaphore` 是信号量，控制许可数量。

示例：

```java
Semaphore semaphore = new Semaphore(100);

if (semaphore.tryAcquire()) {
    try {
        callDownstream();
    } finally {
        semaphore.release();
    }
}
```

适合：

- 限制并发访问数量。
- 保护本地资源。
- 控制同时执行任务数。
- 做轻量舱壁隔离。

注意必须释放许可，否则会造成许可泄漏。

## 三者区别

| 工具 | 核心用途 | 是否可复用 |
| --- | --- | --- |
| CountDownLatch | 等多个任务完成 | 否 |
| CyclicBarrier | 多线程互相等待到齐 | 是 |
| Semaphore | 控制并发许可 | 是 |

## 生产使用注意

注意点：

- `await()` 要考虑超时。
- 异常时要释放资源。
- `Semaphore` 要在 finally 中 release。
- 不要在 Web 请求中无限等待。
- 不要用单机同步工具解决分布式协调。

这些工具只在当前 JVM 内有效。

## 在 eMall 项目中怎么讲？

`CountDownLatch` 可以用于集成测试中等待多个异步消息处理完成。

`Semaphore` 可以用于限制某个下游在单实例内最多并发 100 个请求，避免下游被打爆。

`CyclicBarrier` 在业务服务中相对少见，更常用于并行计算或测试场景。

分布式秒杀限流不能只靠单 JVM `Semaphore`，需要网关、Redis、令牌桶或流量平台。

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
CountDownLatch 用于一个或多个线程等待一批任务完成，是一次性的；CyclicBarrier 用于一组线程
互相等待到齐后一起进入下一阶段，可以复用；Semaphore 用许可数限制并发访问，适合保护本地资源
或做单实例舱壁隔离。

生产中要给 await 设置超时，Semaphore 要 finally release，并且要清楚这些工具只在单 JVM 内有效，
不能替代分布式限流或分布式协调。
```

## 回答评分点

高分答案应该覆盖：

- `CountDownLatch` 一次性等待多个任务。
- `CyclicBarrier` 多线程到齐继续且可复用。
- `Semaphore` 控制并发许可。
- 等待要有超时。
- 单机工具不能解决分布式协调。

# 082 ReentrantLock 和 synchronized 怎么选？

[返回按分类学习面试题](../README.md)

## 题目

`ReentrantLock` 和 `synchronized` 怎么选？

## 先给面试官的短答案

简单同步优先用 `synchronized`，因为语法简单、自动释放、JVM 优化充分。需要可中断获取锁、超时尝试、
公平锁、多条件队列或更灵活控制时，选择 `ReentrantLock`。

选择标准不是哪个更高级，而是业务是否需要 `ReentrantLock` 的额外能力。

## synchronized 的特点

优点：

- 语法简单。
- 自动释放锁。
- 异常时不容易忘记解锁。
- JVM 内置优化。
- 可读性好。

缺点：

- 不能尝试加锁后立即返回。
- 不能设置加锁超时。
- 不能响应中断等待。
- 条件队列能力较弱。

适合简单临界区。

## ReentrantLock 的特点

`ReentrantLock` 是显式锁。

优点：

- `tryLock()`。
- `tryLock(timeout)`。
- `lockInterruptibly()`。
- 可选择公平锁。
- 支持多个 `Condition`。
- 可查询锁状态。

缺点：

- 必须手动释放锁。
- 忘记 `unlock()` 会导致严重故障。
- 代码更复杂。

正确写法：

```java
lock.lock();
try {
    update();
} finally {
    lock.unlock();
}
```

## 什么场景用 synchronized？

适合：

- 临界区很短。
- 锁逻辑简单。
- 不需要超时。
- 不需要中断等待。
- 不需要多个条件队列。

例如保护本地计数器、小缓存元数据、简单状态切换。

## 什么场景用 ReentrantLock？

适合：

- 获取不到锁时要快速失败。
- 等待锁需要超时。
- 等待锁时要支持中断。
- 需要公平锁。
- 需要多个条件队列。
- 需要更复杂同步控制。

例如库存热点保护中，请求等待锁超过 20 ms 就降级或返回重试提示。

## 公平锁选择

`ReentrantLock` 可以创建公平锁：

```java
new ReentrantLock(true)
```

公平锁减少插队，但吞吐通常更低。

默认非公平锁吞吐更好，适合大多数高并发服务。

只有在强公平要求或饥饿风险明显时才考虑公平锁。

## Condition 的价值

`Condition` 可以创建多个等待队列。

相比 `Object.wait/notify`，它更适合复杂同步场景。

例如一个阻塞队列可以分别有：

- notEmpty 条件。
- notFull 条件。

生产业务通常优先使用成熟并发工具，而不是自己写复杂条件同步。

## 在 eMall 项目中怎么讲？

eMall 中简单本地状态保护可以用 `synchronized`。

如果秒杀热点商品需要获取锁失败后快速降级，可以用 `ReentrantLock.tryLock(timeout)`，避免请求无限等待。

如果是跨实例库存一致性，单机锁都不够，要使用数据库条件更新、Redis 原子操作或消息串行化。

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
简单互斥优先 synchronized，因为它语义简单、自动释放、JVM 优化充分。只有当我需要 tryLock、
超时获取、可中断等待、公平锁或多个 Condition 时，才选择 ReentrantLock。ReentrantLock 必须在
finally 中 unlock，否则会造成严重故障。

锁选择还要看边界：单机锁只能保护当前 JVM，不能解决多实例并发。库存这类跨实例一致性问题，
通常需要数据库条件更新、Redis 原子操作或消息化设计。
```

## 回答评分点

高分答案应该覆盖：

- 简单场景优先 `synchronized`。
- `ReentrantLock` 支持超时、中断、公平和 Condition。
- 显式锁必须 finally 解锁。
- 公平锁吞吐通常更低。
- 单机锁不能解决分布式并发。

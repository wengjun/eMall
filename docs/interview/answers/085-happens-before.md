# 085 happens-before 规则是什么？

[返回按分类学习面试题](../README.md)

## 题目

happens-before 规则是什么？

## 先给面试官的短答案

happens-before 是 Java 内存模型中判断可见性和有序性的规则。如果操作 A happens-before 操作 B，
那么 A 的结果对 B 可见，并且 A 的执行顺序在内存语义上先于 B。它不是简单的时间先后，而是内存可见性保证。

常见规则包括程序顺序、锁释放先于后续加锁、volatile 写先于后续读、线程 start 和 join 规则。

## 为什么需要 happens-before？

多线程中，实际执行顺序、CPU 缓存、编译器优化和指令重排序会让代码表现变复杂。

happens-before 提供了一套规则，让程序员判断：

- 一个线程写入是否对另一个线程可见。
- 哪些操作不能被重排序破坏。
- 什么时候读到的数据是安全的。

它是理解 Java 并发的核心概念。

## 程序顺序规则

同一个线程内，前面的操作 happens-before 后面的操作。

示例：

```java
int a = 1;
int b = a + 1;
```

在同一线程内，`a = 1` 对后面的 `b = a + 1` 可见。

注意这是线程内规则，不代表其他线程一定看到。

## 锁规则

对同一把锁：

```text
unlock happens-before subsequent lock
```

一个线程释放锁前的写入，对后续获取同一把锁的线程可见。

这就是 `synchronized` 能保证可见性的原因。

## volatile 规则

对同一个 volatile 变量：

```text
volatile write happens-before subsequent volatile read
```

一个线程写 volatile 变量，另一个线程后续读到这个 volatile 变量时，能看到写线程在写 volatile 前的相关写入。

这让 volatile 能用于状态标记和配置发布。

## 线程 start 规则

调用线程的 `Thread.start()` happens-before 新线程中的任何操作。

示例：

```java
worker.setConfig(config);
thread.start();
```

新线程能看到 start 前已经设置好的状态。

## 线程 join 规则

线程中的所有操作 happens-before 其他线程从 `join()` 成功返回。

这意味着一个线程结束后，join 它的线程能看到它的执行结果。

## 传递性

happens-before 具有传递性。

如果：

```text
A happens-before B
B happens-before C
```

那么：

```text
A happens-before C
```

传递性让复杂并发程序可以通过多个规则组合推导可见性。

## 在 eMall 项目中怎么讲？

配置中心推送新优惠规则时，可以构造不可变 `PromotionRules`，然后赋值给 volatile 引用。

构造对象时的写入 happens-before volatile 写，后续请求线程 volatile 读后就能看到完整配置。

如果没有这些规则，请求线程可能看到未正确发布的对象。

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
happens-before 是 Java 内存模型中的可见性和有序性规则，不是简单的物理时间先后。
如果 A happens-before B，那么 A 的写入对 B 可见，并且内存语义上 A 先于 B。
常见规则有程序顺序、unlock 先于后续 lock、volatile 写先于后续读、Thread.start 和 Thread.join，
并且 happens-before 具有传递性。

理解它能帮助判断 synchronized、volatile、线程启动和线程结束为什么能安全发布数据。
```

## 回答评分点

高分答案应该覆盖：

- happens-before 是内存可见性规则。
- 不等于简单时间顺序。
- 锁规则。
- volatile 规则。
- start/join 规则。
- 传递性。

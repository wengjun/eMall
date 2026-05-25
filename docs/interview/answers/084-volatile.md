# 084 volatile 解决什么问题，不能解决什么问题？

[返回按分类学习面试题](../README.md)

## 题目

`volatile` 解决什么问题，不能解决什么问题？

## 先给面试官的短答案

`volatile` 解决可见性和一定的有序性问题，保证一个线程写入 volatile 变量后，其他线程能及时看到，
并通过内存屏障限制相关指令重排序。但它不能保证复合操作的原子性，例如 `count++` 仍然不是线程安全的。

所以 `volatile` 适合状态标记、开关、配置引用发布，不适合并发计数和复杂状态更新。

## 可见性问题

多线程中，一个线程修改变量，另一个线程不一定立刻看到。

原因是线程可能使用工作内存、CPU 缓存和编译优化。

`volatile` 写入会让修改对其他线程可见。

示例：

```java
private volatile boolean running = true;
```

一个线程修改：

```java
running = false;
```

另一个线程循环读取时能更可靠地看到变化。

## 有序性问题

编译器和 CPU 可能为了性能重排序指令。

`volatile` 会通过内存屏障限制重排序。

典型语义：

- volatile 写之前的普通写不能重排到 volatile 写之后。
- volatile 读之后的普通读写不能重排到 volatile 读之前。

这对安全发布配置对象很重要。

## 不保证复合操作原子性

`count++` 看起来是一行，实际包含：

- 读取 count。
- 加 1。
- 写回 count。

即使 count 是 volatile，多个线程仍然可能同时读到相同旧值，导致丢失更新。

错误示例：

```java
private volatile int count;

void increment() {
    count++;
}
```

并发计数应该用 `AtomicInteger`、`LongAdder` 或锁。

## 适合场景

适合：

- 停止标记。
- 开关变量。
- 配置引用。
- 单写多读状态。
- 双重检查锁中的实例引用。

示例：

```java
private volatile PricingConfig currentConfig;
```

配置整体不可变，替换引用时用 volatile 保证可见。

## 不适合场景

不适合：

- 并发累加。
- 多字段一致更新。
- 读改写复合逻辑。
- 需要互斥的临界区。
- 复杂状态机。

这些场景需要锁、原子类或更高层并发结构。

## volatile 和 synchronized 的区别

`volatile`：

- 保证可见性。
- 保证一定有序性。
- 不保证复合操作原子性。
- 不提供互斥。

`synchronized`：

- 保证互斥。
- 保证可见性。
- 可保护复杂临界区。
- 成本相对更高。

## 在 eMall 项目中怎么讲？

营销配置热更新可以用 volatile 保存不可变配置引用。

```java
private volatile PromotionRules activeRules;
```

更新线程整体替换 `activeRules`，请求线程读取当前引用。

但库存扣减不能靠 volatile，因为库存扣减是读改写，需要数据库条件更新、Redis 原子操作或锁。

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
volatile 解决可见性和有序性问题，通过内存屏障让写入对其他线程可见，并限制相关重排序。
但它不提供互斥，也不能保证 count++ 这类复合操作的原子性。

我会把 volatile 用在状态标记、开关和不可变配置引用发布上。对于计数、库存扣减、多字段一致性更新，
会使用 Atomic、LongAdder、锁、数据库原子更新或事务机制。
```

## 回答评分点

高分答案应该覆盖：

- 解决可见性。
- 提供一定有序性。
- 不保证复合操作原子性。
- 适合状态标记和配置引用。
- 库存扣减不能靠 volatile。

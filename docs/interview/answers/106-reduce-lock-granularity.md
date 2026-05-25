# 106 如何减少锁粒度？

[返回按分类学习面试题](../README.md)

## 题目

如何减少锁粒度？

## 先给面试官的短答案

减少锁粒度就是缩小锁保护的范围和资源维度，让不相关操作不要互相阻塞。常见方式包括缩小同步代码块、
按 key 分段锁、读写锁、拆分热点资源、锁外计算、锁内只做状态切换，避免锁内 IO。

目标是让真正需要互斥的最小代码串行，其他代码并行。

## 缩小同步范围

不要锁整个方法。

低效方式：

```java
synchronized void update() {
    validate();
    callRemote();
    changeState();
}
```

更好方式是锁住真正共享状态：

```java
void update() {
    validate();
    synchronized (lock) {
        changeState();
    }
    publishEvent();
}
```

## 锁外计算

能在锁外完成的计算放到锁外。

锁内只做必要的检查和提交。

这能减少持锁时间。

## 按 key 分段

如果不同商品互不影响，不应该使用全局锁。

可以按 skuId 分段：

```text
lockIndex = hash(skuId) % lockCount
```

不同 key 落到不同锁上，提高并发度。

## 读写锁

读多写少场景可以考虑 `ReadWriteLock`。

多个读可以并发，写需要互斥。

但如果写很多，读写锁收益有限，甚至更复杂。

## 拆分热点资源

热点资源可以拆成多个桶。

例如库存 1000 件，拆成 10 个库存桶，每个桶 100 件。

请求分散到不同桶，降低单行或单锁竞争。

这在秒杀场景很常见。

## 避免锁内 IO

锁内 IO 是扩大锁粒度的常见原因。

应该避免：

- 锁内查数据库。
- 锁内调用 HTTP。
- 锁内发送 MQ。
- 锁内写大日志。

锁内只做内存状态的最小修改。

## 在 eMall 项目中怎么讲？

秒杀库存不能用一个全局锁保护所有商品。

可以：

- 按商品分锁。
- 热点商品拆库存桶。
- 数据库条件更新。
- Redis 原子预扣。
- MQ 削峰串行化热点。

锁粒度要贴近业务冲突粒度。

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
减少锁粒度的核心是只锁真正共享且需要互斥的最小资源。做法包括缩小同步块、锁外计算、按 key
分段锁、读写锁、拆分热点资源和库存桶，并且避免锁内 IO。锁粒度要和业务冲突粒度一致。

在电商库存场景，不同商品不应竞争同一把锁，热点商品还可以拆桶或用队列削峰。
```

## 回答评分点

高分答案应该覆盖：

- 缩小同步代码块。
- 锁外计算。
- 按 key 分段。
- 避免锁内 IO。
- 热点资源拆分。

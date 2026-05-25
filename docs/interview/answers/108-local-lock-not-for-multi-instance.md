# 108 单机锁为什么不能解决多实例并发？

[返回按分类学习面试题](../README.md)

## 题目

单机锁为什么不能解决多实例并发？

## 先给面试官的短答案

单机锁只在当前 JVM 进程内有效。微服务多实例部署时，每个实例都有自己的内存和锁，实例 A 的锁无法阻止实例 B 同时修改同一资源。
所以库存扣减、优惠券领取、订单幂等等跨实例共享资源，不能只靠 `synchronized`、`ReentrantLock` 或本地缓存锁。

多实例并发要依赖数据库唯一键、条件更新、分布式锁、Redis 原子操作、消息队列或状态机幂等。

## 单机锁的边界

本地锁保护的是当前进程内共享对象。

例如：

```java
synchronized (lock) {
    reserveStock();
}
```

只对当前 JVM 内线程有效。

其他 Pod、其他机器、其他进程完全不知道这把锁。

## 多实例场景

Kubernetes 中订单服务可能有 20 个副本。

同一个商品的扣库存请求会被负载均衡分到不同实例。

每个实例都能拿到自己的本地锁，然后同时执行扣减。

这就可能超卖。

## 哪些场景不能靠单机锁？

典型场景：

- 库存扣减。
- 优惠券领取。
- 防重复下单。
- 支付回调幂等。
- 账户余额变更。
- 分布式任务调度。
- 全局唯一资源分配。

只要资源跨实例共享，就不能只靠单机锁。

## 更可靠的方式

常用方式：

- 数据库唯一键。
- 数据库条件更新。
- 乐观锁版本号。
- Redis Lua 原子脚本。
- 分布式锁。
- MQ 串行化。
- 幂等表。
- 状态机。

很多时候数据库约束比分布式锁更可靠。

## 在 eMall 项目中怎么讲？

库存扣减应该使用：

```sql
update inventory
set available = available - 1
where sku_id = ? and available > 0
```

通过数据库原子条件更新防止超卖。

本地锁最多用于减少单实例内竞争，不能作为最终一致性保障。

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
单机锁只在当前 JVM 内有效，多实例部署时每个实例都有自己的锁，无法阻止其他实例同时修改共享资源。
所以库存、优惠券、支付回调幂等等跨实例资源不能靠 synchronized 或 ReentrantLock。

真正的并发控制要落到共享一致性点，比如数据库唯一键和条件更新、Redis Lua、幂等表、状态机、
MQ 串行化或经过严格设计的分布式锁。
```

## 回答评分点

高分答案应该覆盖：

- 单机锁只保护当前 JVM。
- 多实例各有自己的锁。
- 跨实例资源不能靠本地锁。
- 数据库条件更新和唯一键更可靠。
- 能联系库存和支付幂等。

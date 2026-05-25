# 089 AtomicInteger 和 LongAdder 如何取舍？

[返回按分类学习面试题](../README.md)

## 题目

`AtomicInteger` 和 `LongAdder` 如何取舍？

## 先给面试官的短答案

`AtomicInteger` 基于单个变量 CAS，适合低竞争、需要立即获取精确值或需要 CAS 条件更新的场景。
`LongAdder` 通过分散热点到多个 cell 降低竞争，适合高并发计数、指标累加，但读取汇总值不是强一致瞬时值。

简单说：精确状态更新用 Atomic，高并发统计计数用 LongAdder。

## AtomicInteger 特点

`AtomicInteger` 维护一个原子 int 值。

常见操作：

```java
incrementAndGet()
compareAndSet(expected, update)
```

优点：

- 语义简单。
- 读取值精确。
- 支持 CAS 条件更新。
- 适合状态转换。

缺点：

- 高竞争下多个线程争抢同一个变量。
- CAS 失败重试会增加 CPU 消耗。

## LongAdder 特点

`LongAdder` 会在竞争激烈时把计数分散到多个 cell。

不同线程可能更新不同 cell，减少对单点的争抢。

读取时把 base 和 cells 汇总。

优点：

- 高并发累加吞吐高。
- 降低 CAS 热点竞争。

缺点：

- 不支持条件 CAS。
- `sum()` 不是严格线性一致快照。
- 内存占用更高。

## 为什么 LongAdder 更适合指标？

监控计数通常关注趋势和吞吐，不要求每次读取都是严格瞬时一致。

例如：

- 请求次数。
- 限流统计。
- 命中次数。
- 错误次数。
- 业务埋点。

这些场景中，`LongAdder` 能减少热点竞争。

## 为什么库存不能用 LongAdder？

库存扣减需要严格条件判断：

```text
if stock > 0 then stock = stock - 1
```

这不是简单累加指标。

`LongAdder` 不适合做库存精确状态控制。

库存应使用数据库条件更新、Redis Lua、分布式库存服务或事务机制。

## 选择规则

选择 `AtomicInteger`：

- 需要精确当前值。
- 需要 CAS 条件更新。
- 竞争不高。
- 表达状态机或开关。

选择 `LongAdder`：

- 高并发累加。
- 统计指标。
- 允许读取时轻微非瞬时一致。
- 不需要基于当前值做条件更新。

## 在 eMall 项目中怎么讲？

eMall 网关统计请求数、限流命中数、错误数，可以用 `LongAdder`。

本地状态开关、简单版本号、CAS 状态切换可以用 `AtomicInteger`。

商品库存不能用这两个直接解决跨实例一致性，需要持久化原子更新或库存中心。

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
AtomicInteger 基于单变量 CAS，适合低竞争、精确读取和条件更新；LongAdder 通过分散到多个 cell
降低热点竞争，适合高并发统计累加，但 sum 不是严格线性一致快照，也不支持 compareAndSet。

所以指标计数、QPS 统计、错误次数用 LongAdder 更合适；状态更新、版本号、CAS 条件修改用 Atomic。
库存这类业务一致性问题不能用单机原子类直接解决。
```

## 回答评分点

高分答案应该覆盖：

- `AtomicInteger` 单变量 CAS。
- `LongAdder` 分散热点。
- `LongAdder` 适合高并发统计。
- `AtomicInteger` 适合精确状态和 CAS。
- 库存不能靠单机原子类。

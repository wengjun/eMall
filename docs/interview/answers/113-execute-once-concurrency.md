# 113 并发下如何实现只执行一次？

[返回按分类学习面试题](../README.md)

## 题目

并发下如何实现只执行一次？

## 先给面试官的短答案

只执行一次要先明确范围：单 JVM、单数据库资源还是分布式多实例。单 JVM 可以用原子变量、锁或 `ConcurrentHashMap`；
分布式场景通常用数据库唯一键、幂等表、状态机、消息去重或分布式锁加资源端约束。

真正可靠的做法是让“执行结果”可幂等，而不是只依赖“执行过程”只发生一次。

## 单 JVM 只执行一次

可以用 `AtomicBoolean`：

```java
if (started.compareAndSet(false, true)) {
    startJob();
}
```

这只在当前进程有效。

多实例部署时不够。

## 数据库唯一键

分布式防重复常用唯一键。

例如：

```sql
insert into idempotent_record(request_id, status) values (?, 'PROCESSING')
```

`request_id` 唯一。

只有插入成功的实例执行，插入失败的实例查询已有结果。

## 状态机

状态机通过状态条件控制只执行一次。

例如订单支付：

```sql
update orders
set status = 'PAID'
where order_id = ? and status = 'CREATED'
```

只有一个请求能把 `CREATED` 改成 `PAID`。

后续重复请求发现状态已变更，返回幂等结果。

## 消息消费只执行一次

消息队列通常提供至少一次投递。

消费者要自己做幂等：

- 消费记录表。
- 业务唯一键。
- 状态机条件更新。
- 去重 key。

不要假设 MQ 永远只投递一次。

## 分布式锁的角色

分布式锁可以减少并发进入，但不能作为唯一保障。

因为锁可能失效。

更稳妥：

```text
分布式锁降低冲突 + 数据库唯一键兜底
```

## 在 eMall 项目中怎么讲？

支付回调只处理一次：

- `payment_no` 建唯一键。
- 订单状态从 `UNPAID` 条件更新为 `PAID`。
- 重复回调查询已支付结果并返回成功。
- 发送消息使用 outbox 防重复。

这样即使并发回调，也不会重复改订单或重复发货。

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
只执行一次要先定义范围。单 JVM 可以用 AtomicBoolean 或本地锁，多实例必须依赖共享一致性点。
我通常用唯一键、幂等表和状态机条件更新，让只有一个请求能创建处理记录或推进状态。

分布式锁只能降低并发冲突，不能替代最终幂等。关键是让重复请求返回同一结果，而不是幻想网络中绝对只执行一次。
```

## 回答评分点

高分答案应该覆盖：

- 先定义单机还是分布式范围。
- 单机可用 Atomic 或锁。
- 分布式用唯一键、幂等表、状态机。
- MQ 要消费幂等。
- 结果幂等比过程唯一更可靠。

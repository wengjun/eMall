# 114 如何设计幂等和并发安全的组合方案？

[返回按分类学习面试题](../README.md)

## 题目

如何设计幂等和并发安全的组合方案？

## 先给面试官的短答案

组合方案通常包括幂等 key、唯一键或幂等表、状态机条件更新、事务边界、消息 outbox、重试策略和补偿机制。
并发安全保证同一时刻不会写错，幂等保证重复请求返回同一结果。两者要一起设计。

电商核心链路不能只靠锁，必须有资源端约束和业务状态机。

## 幂等 key

每个请求需要稳定唯一标识。

来源可以是：

- 客户端请求号。
- 支付流水号。
- 订单号。
- 消息 ID。
- 业务组合键。

没有幂等 key，就很难识别重复请求。

## 幂等表

幂等表记录请求处理状态：

```text
request_id
status
result
created_at
updated_at
```

状态可以是：

- PROCESSING。
- SUCCESS。
- FAILED。

并发请求先尝试插入幂等记录，只有成功者执行业务。

## 状态机条件更新

业务状态推进要带条件。

示例：

```sql
update orders
set status = 'PAID'
where order_id = ? and status = 'UNPAID'
```

这能防止重复支付回调重复推进状态。

## 唯一键兜底

唯一键是资源端最后防线。

例如：

- `client_request_id unique`。
- `payment_no unique`。
- `user_id + coupon_id unique`。
- `message_id unique`。

即使应用层并发控制失效，数据库仍能防重复。

## Outbox

本地事务内同时写业务表和 outbox 事件表。

事务提交后由后台发布消息。

这样能避免业务成功但消息发送失败。

消费者再用幂等保证重复消息安全。

## 重试和补偿

重试必须：

- 只重试幂等操作。
- 有次数限制。
- 有退避和抖动。
- 能查询已有结果。

补偿用于处理半成功状态，例如库存已扣但订单创建失败。

## 在 eMall 项目中怎么讲？

创建订单可以这样设计：

- 客户端传 `requestId`。
- 订单表对 `requestId` 建唯一键。
- 库存扣减使用条件更新。
- 订单状态用状态机推进。
- 订单创建成功写 outbox。
- 重复请求查询并返回已有订单。

这样能同时抗并发、抗重试和抗消息重复。

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
幂等和并发安全要组合设计。入口用稳定幂等 key，资源端用唯一键或幂等表防重复，业务推进用状态机条件更新，
本地事务写 outbox 保证消息可靠，消费者再做消息幂等。重试必须有次数、退避和查询已有结果能力。

锁只能减少冲突，不能作为最终正确性保证。最终要靠数据库约束、状态机、幂等记录和补偿闭环。
```

## 回答评分点

高分答案应该覆盖：

- 幂等 key。
- 唯一键或幂等表。
- 状态机条件更新。
- Outbox 和消息幂等。
- 重试和补偿。

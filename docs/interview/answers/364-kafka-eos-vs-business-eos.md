# 364 Kafka 的 exactly-once 为什么不等于业务 exactly-once？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Kafka 的 exactly-once 主要保证 Kafka 内部生产、消费、写回 Kafka 的原子性和幂等性。业务
exactly-once 涉及数据库、缓存、第三方支付、库存服务和外部接口，边界远大于 Kafka。

所以 Kafka exactly-once 不能替代业务幂等、唯一约束和状态机。

## Kafka 能保证什么

Kafka 事务可以保证：

- 消费输入 Topic。
- 处理后写输出 Topic。
- 提交 offset。
- 这些动作在 Kafka 内部形成事务边界。

它适合 Kafka 到 Kafka 的流处理。

## Kafka 不能自动保证什么

不能自动保证：

- 数据库写入不重复。
- 第三方支付不重复扣款。
- Redis 状态不重复更新。
- HTTP 下游接口不重复调用。
- 库存不会重复扣减。

这些在 Kafka 事务边界之外。

## 业务 exactly-once

业务上要靠：

- 幂等号。
- 唯一约束。
- 状态机条件更新。
- 去重表。
- Outbox。
- 事务消息或补偿。
- 对账。

这才是端到端正确性的基础。

## 在 eMall 项目中怎么讲？

eMall 支付成功事件即使 Kafka 保证消息写入不重复，也不能证明订单不会重复更新或退款不会重复发起。

订单服务必须使用支付单号做幂等，状态从 `PENDING_PAYMENT` 条件更新到 `PAID`，重复消息只返回
已处理结果。

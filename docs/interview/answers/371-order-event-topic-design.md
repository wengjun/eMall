# 371 如何设计订单事件 topic？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

订单事件 Topic 要围绕事件语义、分区键、消息 schema、可靠性、顺序性、订阅方和治理能力设计。
核心原则是同一订单事件按 `orderId` 路由到同一 Partition，保证单订单状态流转有序。

不要只设计一个随意的消息队列，要把它当成跨服务契约。

## Topic 划分

常见划分方式：

- 按领域事件划分，例如 `order-created`、`order-paid`。
- 按订单事件总线划分，例如 `order-events`。
- 按环境和版本区分。
- 按业务重要性区分核心事件和日志事件。

Topic 过细会增加治理成本，过粗会增加消费过滤和 schema 复杂度。

## 分区键

订单事件通常选 `orderId` 作为 key。

好处：

- 同一订单进入同一 Partition。
- 单订单状态事件有序。
- 消费端更容易做状态机处理。

如果按 `userId`，可以保证用户维度顺序，但订单维度可能不够清晰。

## 可靠性配置

核心配置：

- `acks=all`。
- 开启 Producer 幂等。
- 合理副本数。
- 设置 `min.insync.replicas`。
- 发送失败要重试和告警。
- 使用 Outbox 避免本地事务和消息发送不一致。

订单事件不能静默丢失。

## 电商系统实践

大型电商系统可以设计 `order-events-v1` Topic，key 使用 `orderId`，事件类型字段区分 created、paid、
cancelled、shipped 和 refunded。

订单服务本地事务写入订单表和 Outbox 表，Relay 再投递 Kafka。消费者通过事件 ID 做幂等。

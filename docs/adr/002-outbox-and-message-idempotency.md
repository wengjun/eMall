# ADR 002：Outbox 和消息消费幂等

[文档索引](../README.md) | [架构设计](../architecture.md)

## 背景

订单、库存、支付、搜索、分析和数仓之间依赖异步事件。生产环境中 Kafka 至少一次投递是常态，
消费者会遇到重复消息、处理失败、进程崩溃和重平衡。如果消费者只靠内存去重，服务重启后会重复处理。

## 决策

事件发布使用 Outbox，事件消费使用落库幂等模板。

- 业务服务在本地事务内同时写业务数据和 Outbox。
- 发布任务异步 claim Outbox 记录并发送 Kafka。
- 消费者使用 `processed_message` 表记录 `PROCESSING`、`PROCESSED`、`FAILED`、`DEAD`。
- 业务消费者只写业务处理逻辑，幂等、失败计数、死信标记由 common 模板处理。
- 内存仓储仅用于测试或 memory profile，生产消费者使用 MyBatis Plus 仓储。

## 备选方案

- 直接发 Kafka：代码简单，但业务成功后发消息失败会丢事件。
- Kafka 事务：适合 Kafka 内部一致性，但不能覆盖数据库本地事务。
- 每个模块复制幂等逻辑：短期快，长期容易出现状态语义不一致。

## 优点

- 不丢事件，不重复执行业务副作用。
- 失败重试和死信状态可观测。
- search、fulfillment、order、inventory、analytics、data-warehouse 可以复用同一套消费语义。
- 消费者崩溃后可通过 processing lease 平滑恢复。

## 代价

- 每个消费服务都需要 `processed_message` 表。
- 消费逻辑需要区分可重试失败和最终死信。
- 消费者吞吐会多一次本地数据库写入，需要按 topic 和分区水平扩展。

## 落地位置

- `common/src/main/java/com/emall/common/messaging/MessageConsumerTemplate.java`
- `common/src/main/java/com/emall/common/messaging/MybatisPlusProcessedMessageRepositorySupport.java`
- `search/src/main/java/com/emall/search/messaging/ProductEventConsumer.java`
- `fulfillment/src/main/java/com/emall/fulfillment/messaging/OrderEventConsumer.java`
- `order/src/main/java/com/emall/order/messaging/PaymentEventConsumer.java`
- `inventory/src/main/java/com/emall/inventory/messaging/OrderFailureEventConsumer.java`
- `analytics/src/main/java/com/emall/analytics/CoreBusinessEventConsumer.java`
- `data-warehouse/src/main/java/com/emall/datawarehouse/BusinessEventConsumer.java`

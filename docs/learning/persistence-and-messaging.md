# 持久化与消息

[代码导读首页](README.md) | [上一篇：下单与恢复](checkout-and-recovery.md) | [下一篇：运行配置与验证](runtime-and-verification.md)

本文跟踪持久化和事件链路的实际入口；Mapper 编写约定以[持久层规范](../persistence-conventions.md)为准，
Outbox 的设计取舍见 [ADR 002](../adr/002-outbox-and-message-idempotency.md)。

## 从领域对象到 MyBatis-Plus

从 [MybatisPlusOrderRepository](../../order/src/main/java/com/emall/order/repository/MybatisPlusOrderRepository.java)
依次进入 [OrderEntity](../../order/src/main/java/com/emall/order/repository/OrderEntity.java) 和
[OrderMapper](../../order/src/main/java/com/emall/order/repository/OrderMapper.java)。

领域对象与表实体不是同一个角色：业务层使用订单状态、金额和时间语义；仓储负责字段映射，
例如将领域时间 `Instant` 按 UTC 转换为持久化使用的 `LocalDateTime`。Mapper 继承 `BaseMapper<OrderEntity>`，
常规增删查改由 MyBatis-Plus 提供，业务层不接触结果集或连接对象。

`@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc")` 中的 `jdbc` 是现有存储模式名称，
不表示这个类仍在使用 `JdbcTemplate`。判断实现应看 Mapper 和方法体，不要仅凭配置值名称。

## 条件更新比生成方法更重要

订单仓储的 `updateStatus` 不是无条件 `updateById`。以下是其中的条件更新片段，`entity` 来自领域对象映射：

```java
return orderMapper.update(null,
        new UpdateWrapper<OrderEntity>().set("status", entity.getStatus())
                .set("failure_reason", entity.getFailureReason()).set("updated_at", entity.getUpdatedAt())
                .eq("order_id", orderId).eq("status", expectedStatus.name())) == 1;
```

`eq("status", expectedStatus.name())` 把状态前置条件交给数据库校验。受影响行数不是 1 时，调用方不能宣称迁移成功，
应重新检查当前状态。MyBatis-Plus 减少 SQL 样板，但不会自动补上这类业务条件。

同样，`save` 与 `updateStatus` 的语义不同：前者处理记录保存，后者表达期望状态下的迁移。
不要因为都能写表，就把状态机路径全部替换为通用 `save`。

库存扣减、领取事件及聚合序号分配还涉及算术、竞争条件或查询顺序。需要明确 SQL 的部分仍留在强类型 Mapper 中，
不是为了“全用 ORM”而牺牲语义。具体边界见[持久层规范](../persistence-conventions.md)。

## 订单事件如何进入 Outbox

下单工作流在本地事务里调用 `appendEvent`，服务侧的
[MybatisPlusOutboxRepository](../../order/src/main/java/com/emall/order/repository/MybatisPlusOutboxRepository.java)
继承公共 [MybatisPlusOutboxRepositorySupport](../../common/src/main/java/com/emall/common/outbox/MybatisPlusOutboxRepositorySupport.java)。

公共实现将 `OutboxEvent` 转换为表记录，序列化 payload，并为新事件分配聚合版本。
服务自己的 [OrderOutboxEventMapper](../../order/src/main/java/com/emall/order/repository/OrderOutboxEventMapper.java)
连接对应事件表；同一物理事务中的订单写入与 Outbox 写入才构成原子边界。

不应在事务内直接发 Kafka 并假定它与 MySQL 一起提交，也不能把写入另一个数据源的事件表称为同一本地事务。
分片模式下需同时核对路由上下文、数据源及事务管理器，不能只看两行相邻的 `save` 调用。

## 从领取到异步发布

发布入口是 [OutboxPublisher](../../order/src/main/java/com/emall/order/messaging/OutboxPublisher.java)，
主要流程位于 [OutboxPublisherSupport](../../common/src/main/java/com/emall/common/outbox/OutboxPublisherSupport.java)。
沿以下方法阅读完整的领取、发送及结果写回路径：

| 方法 | 关键行为 |
| --- | --- |
| `claimAcrossShards` | 按游标轮转扫描有限分片，调用仓储领取候选事件 |
| `claimPublishable` | 按状态、重试时间或过期租约条件竞争更新，只有领取成功的事件才返回给发布器 |
| `publishOne` | 校验事件契约，以 `aggregateId` 为 Kafka key 异步发送 |
| `saveInOriginShard` | 在异步回调里显式回到事件原分片，再保存发布结果 |
| `failedEvent` | 记录失败原因和退避时间，超过重试上限转入死信状态 |

`KafkaTemplate.send` 返回 `CompletableFuture`；这里先发起一批发送，再用 `allOf(...).join()` 等待批次结束。
“异步发送”不等于整个定时任务不等待，也不代表可以无限增大批次。

异步完成回调不应假定自动继承调用线程的分片 ThreadLocal，所以实现显式保留 `shardIndex`。
同理，外层线程的本地事务不能自动跨到 Kafka 回调线程。

Kafka 已接收消息、数据库尚未记录成功时进程退出，仍可能导致重复发送。领取租约和状态表不是端到端 exactly-once 证明，
消费者仍须处理重复事件。

## 消费事务与重试

[PaymentEventConsumer](../../order/src/main/java/com/emall/order/messaging/PaymentEventConsumer.java) 是一个实际入口：
`@KafkaListener` 收到消息后解析事件，定位用户分片，再调用
[MessageConsumerTemplate](../../common/src/main/java/com/emall/common/messaging/MessageConsumerTemplate.java)。

模板在配置了事务管理器时创建业务事务与失败记录事务，两者使用 `REQUIRES_NEW`：

1. 在业务事务中验证契约，尝试登记处理状态，并检查聚合版本。
2. 执行业务 handler，成功后标记消息已处理。
3. handler 抛出异常时回滚业务路径；另开事务记录失败次数，达到上限后记录死信并抛出对应异常。

独立失败事务避免“业务回滚连失败次数也一起回滚”。但它只处理数据库状态，Kafka 重投与 DLT 行为还要结合
监听容器错误处理配置和真实 Kafka 测试判断。

特别注意：模板在事务内调用 `handler.accept(event)`。当前 `PaymentEventConsumer` 的 handler 会进入 `OrderService.pay`，
因此不能仅因 `pay` 方法没有 `@Transactional` 就断言该路径的下游调用在事务外。
阅读任何带远程调用的 handler，都需要检查整个调用栈及传播行为，不能把模板当成所有流程都适用的短事务封装。

## 验证入口

| 关注点 | 测试 |
| --- | --- |
| 映射和条件更新 | [MybatisPlusOrderRepositoryTest](../../order/src/test/java/com/emall/order/repository/MybatisPlusOrderRepositoryTest.java) |
| Outbox 仓储行为 | [MybatisPlusOutboxRepositorySupportTest](../../common/src/test/java/com/emall/common/outbox/MybatisPlusOutboxRepositorySupportTest.java) |
| 发布分支及异步结果处理 | [OutboxPublisherSupportTest](../../common/src/test/java/com/emall/common/outbox/OutboxPublisherSupportTest.java) |
| 真实 Kafka 发布 | [KafkaOutboxPublisherIT](../../common/src/test/java/com/emall/common/integration/KafkaOutboxPublisherIT.java) |
| 消费成功、回滚和失败记录 | [MessageConsumerTransactionIT](../../common/src/test/java/com/emall/common/integration/MessageConsumerTransactionIT.java) |
| MySQL 库存并发与模式切换 | [InventoryRepositoryIT](../../inventory/src/test/java/com/emall/inventory/repository/InventoryRepositoryIT.java) |

Mock Mapper 测试不能证明 SQL 在 MySQL 上正确；真实组件测试也不能单独证明生产容量。
执行条件和发布门禁统一见[集成测试](../integration-testing.md)与[生产检查清单](../production-checklist.md)。

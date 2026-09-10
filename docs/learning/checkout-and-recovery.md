# 下单与恢复

[代码导读首页](README.md) | [下一篇：持久化与消息](persistence-and-messaging.md)

本文只跟踪本工程的请求入口和恢复代码。接口字段以
[Web/App 下单契约](../api/web-app-checkout.openapi.yml)为准，业务数据流图见
[设计深度说明](../design-deep-dive.md)。

## 从请求进入业务

先打开 [OrderController](../../order/src/main/java/com/emall/order/api/OrderController.java) 的
`@PostMapping` 重载，而不是供测试直接调用的简化重载。

`CreateOrderRequest` 是 Java record；Spring 完成 JSON 反序列化，`@Valid` 触发字段约束校验。
控制器随后通过 `AuthorizationGuard` 校验请求用户，组合 `OrderClientContext` 和 `ClientTrustContext`，
再调用 `OrderService.create`。设备、渠道等客户端信息不等于可信身份。

HTTP 接口返回 `202 Accepted`，不能仅凭响应码认定库存已经预占成功；还要读取返回订单的业务状态。
Web 与 App 共用这条后端链路，不需要各维护一套下单事务。

登录与账户生命周期由 `identity` 负责，`user` 是档案投影服务，不是第二个账户权威来源。
入口鉴权的过滤器和服务内权限检查见[运行配置与验证](runtime-and-verification.md#入口身份与线程模型)。

## 幂等与分片在哪里生效

[OrderService](../../order/src/main/java/com/emall/order/service/OrderService.java) 的 `create` 依次执行账户访问校验、
提交频率限制、请求摘要计算和分片路由，然后进入
[IdempotencyExecutor](../../common/src/main/java/com/emall/common/idempotency/IdempotencyExecutor.java)。

这里有三个不同层次，调试时不要混为一谈：

| 层次 | 当前入口 | 需要观察的行为 |
| --- | --- | --- |
| 请求执行状态 | `IdempotencyService.begin` | 已成功则回放，处理中或终态失败则拒绝，新请求才执行业务 |
| 业务记录复用 | `createIdempotent` | 按 `requestId` 查已有订单，并校验用户、商品、数量及客户端上下文 |
| 数据位置 | `ShardRoutingOperations`、`ShardRouteIndex` | 用用户路由键进入分片，按订单或请求索引定位后续访问 |

幂等摘要包含用户、SKU、数量、客户端类型、设备和渠道。因此“同一个请求 ID”不意味着可以更换参数重放。
执行器通过 `Supplier<T>` 接收首次执行逻辑，通过 `Function` 接收回放和摘要逻辑；它不负责把所有下游调用包成事务。

成功记录保存的是结果摘要；订单回放仍需读取业务记录。不要把幂等记录误认为完整的 HTTP 响应缓存。
路由索引的持久目录和缓存边界见[数据平台](../data-platform.md)。

## 下单事务的真实边界

继续进入 [OrderCreateWorkflow.create](../../order/src/main/java/com/emall/order/workflow/OrderCreateWorkflow.java)。
当前 HTTP 下单流程没有用外层 `@Transactional` 包住价格、优惠和库存调用，而是按以下顺序执行：

1. 启动或取得持久化 Saga，使用 Saga 已确定的订单 ID。
2. 查询价格与促销，校验应付金额并执行风控。
3. 记录优惠券预占意图，再调用营销服务；记录库存预占意图，再调用库存服务。
4. 根据资源结果决定订单为 `CREATED` 或 `PENDING_RETRY`，必要时释放已预占优惠券。
5. 在 `localTransaction.execute("create", ...)` 内保存订单和本地路由记录；只有 `CREATED` 分支写入创建事件。
6. 本地写入返回后推进 Saga 到 `ORDER_PERSISTED`，再标记完成。

这里有两个边界：远程资源操作不属于订单库的本地写事务，`PENDING_RETRY` 也不等于 `CREATED`。
库存未成功预占时，不会发布相同的订单创建事件。

[OrderLocalTransaction](../../order/src/main/java/com/emall/order/transaction/OrderLocalTransaction.java) 使用
Spring `TransactionTemplate` 执行局部 `Supplier`，配置项 `emall.order.local-transaction-timeout-seconds`
默认是 3 秒，并记录 `emall_order_local_transaction_duration`。

这里要读懂两个 Java/Spring 细节：

- lambda 捕获的数据必须是 final 或 effectively final；工作流用 `finalPromotionQuote` 固定进入本地写入的报价。
- `TransactionTemplate` 默认传播行为是 `REQUIRED`。无外层事务时创建事务，有外层事务时可能加入已有事务；
  “使用了模板”本身不保证任意调用方都是短事务。缺少事务管理器的直接执行分支也不提供数据库原子性。

审查其他入口时仍要追溯调用方，尤其是[消息消费事务](persistence-and-messaging.md#消费事务与重试)，
不能把 HTTP 下单入口的边界推广到所有业务方法。

## 失败后到哪里继续

[OrderSagaCoordinator](../../order/src/main/java/com/emall/order/saga/OrderSagaCoordinator.java) 负责阶段推进、恢复与补偿，
[OrderSagaStateService](../../order/src/main/java/com/emall/order/saga/OrderSagaStateService.java) 负责持久化状态。
恢复调度入口是 [OrderSagaRecoveryJob](../../order/src/main/java/com/emall/order/saga/OrderSagaRecoveryJob.java)。

读代码时用故障窗口定位处理分支，比重复背诵 Saga 定义更有用：

| 故障窗口 | 当前代码的处理 |
| --- | --- |
| 发起预占后未收到结果 | 补偿前查询预占状态；查询失败或结果不确定，不把资源视为已安全释放 |
| 订单已落库，Saga 完成状态未写入 | 恢复或补偿检查订单是否存在，避免直接释放有效订单资源 |
| 优惠券已被消费或库存已确认 | 不按普通未使用预占盲目释放，保留待处理状态 |
| 补偿无法确认 | 保存失败原因及重试信息，达到尝试上限后保留人工处理状态 |
| 在线请求与恢复任务并发推进 | Saga 仓储按版本条件写入；冲突不能覆盖为旧状态 |

版本条件更新见
[MybatisPlusOrderSagaRepository](../../order/src/main/java/com/emall/order/saga/MybatisPlusOrderSagaRepository.java)。
资源查询和释放由各自的 `InventoryClient`、`MarketingClient` 完成，不是跨服务数据库回滚。

## 用测试对照阅读

- [OrderControllerTest](../../order/src/test/java/com/emall/order/api/OrderControllerTest.java)：控制器请求与响应行为。
- [OrderServiceTest](../../order/src/test/java/com/emall/order/service/OrderServiceTest.java)：下单、回放及业务状态分支。
- [OrderLocalTransactionTest](../../order/src/test/java/com/emall/order/transaction/OrderLocalTransactionTest.java)：局部执行前后事务状态与计时指标。
- [OrderSagaCoordinatorTest](../../order/src/test/java/com/emall/order/saga/OrderSagaCoordinatorTest.java)：资源查询、补偿和恢复判断。
- [OrderSagaOptimisticConcurrencyTest](../../order/src/test/java/com/emall/order/saga/OrderSagaOptimisticConcurrencyTest.java)：并发推进时的状态保护。

局部事务测试使用测试事务管理器，不能替代真实 MySQL 的提交、回滚和并发验收。
执行方式统一见[集成测试说明](../integration-testing.md)。

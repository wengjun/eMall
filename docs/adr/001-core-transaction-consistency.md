# ADR 001：核心交易一致性方案

[文档索引](../README.md) | [架构设计](../architecture.md)

## 背景

下单链路会同时涉及订单、库存、优惠券、支付和事件发布。若使用全局强事务或 2PC，链路会变长，
数据库锁持有时间会增加，下游抖动会直接拖垮核心交易入口，不适合大促和高并发场景。

## 决策

核心交易链路采用本地事务、资源预占、Outbox、MQ、幂等消费和补偿任务组合。

- 订单创建只在订单库本地事务内写订单和 Outbox。
- 库存和优惠券使用预占、确认、释放。
- 支付成功后触发订单支付、库存确认和优惠券确认。
- 下游失败时进入 `PENDING_RETRY`，由补偿任务和事件重试推动最终一致。
- 所有外部请求和消息消费都必须具备幂等。

## 备选方案

- 强 2PC：一致性最强，但锁持有时间长，吞吐低，故障恢复复杂。
- 普通异步事件：吞吐高，但库存和优惠券这种资源型操作容易出现超卖或重复使用。
- 重型 Saga 框架：表达能力强，但引入成本高，当前工程用显式状态机和补偿更容易讲清楚。

## 优点

- 高并发入口不被跨服务事务拖慢。
- 单个下游故障不会直接破坏订单主链路。
- 状态、补偿和事件都能通过指标观察。
- 面试时可以清楚解释一致性、可用性和吞吐之间的取舍。

## 代价

- 业务代码必须显式处理中间状态。
- 需要补偿任务、幂等表、Outbox 和消费者重试。
- 查询侧可能短时间看到最终一致延迟。

## 落地位置

- `order/src/main/java/com/emall/order/workflow/OrderCreateWorkflow.java`
- `order/src/main/java/com/emall/order/service/OrderService.java`
- `marketing/src/main/java/com/emall/marketing/service/MarketingService.java`
- `inventory/src/main/java/com/emall/inventory/service/InventoryService.java`
- `common/src/main/java/com/emall/common/outbox`
- `common/src/main/java/com/emall/common/messaging`

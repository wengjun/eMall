# 226 如何保证同一订单事件顺序？

[返回按分类学习面试题](../README.md)

## 题目

如何保证同一订单事件顺序？

## 先给面试官的短答案

要保证同一订单事件顺序，生产者应使用同一个业务 key，例如 `orderId`，让同一订单的事件进入同一个 partition。
消费者侧还要按订单状态机和事件版本校验，防止重复、乱序和旧事件覆盖新状态。

仅依赖 MQ 顺序不够，业务状态机也要防护。

## MQ 层顺序

常见做法：

- 以 `orderId` 作为消息 key。
- 同一 key 路由到同一 partition。
- 单 partition 内保持顺序。
- 消费者对同一 partition 顺序消费。

这可以保证同一订单事件在 MQ 层有序。

## 业务层顺序

业务仍要校验：

- 当前订单状态是否允许变更。
- 事件版本是否大于已处理版本。
- 事件是否已经处理过。
- 旧事件是否应该丢弃。

因为重试、死信重放和人工补偿都可能打破原始顺序。

## 状态机示例

订单状态可以限制：

```text
CREATED -> PAID -> FULFILLED -> COMPLETED
CREATED -> CANCELED
PAID -> REFUNDED
```

如果收到 `PAID` 后又收到旧的 `CREATED` 事件，状态机应拒绝回退。

## 在 eMall 项目中怎么讲？

订单服务发布订单事件时使用 `orderId` 作为 MQ key。

履约服务消费时记录已处理事件版本，只有版本更高且状态迁移合法时才更新履约状态。

## 深度增强：一致性和补偿图

![交易一致性、对账和补偿闭环](../assets/consistency-compensation-loop.svg)

分布式一致性题要先区分事实来源、状态流转和补偿责任。
订单、库存、支付、优惠和消息不可能总靠一个本地事务完成，
所以要用幂等、状态机、Outbox、重试、对账和补偿形成闭环。

## 深度增强：Java 17 状态机示例

```java
enum TradeState {
    INIT,
    RESERVED,
    PAID,
    CLOSED
}

record TradeTransition(TradeState from, TradeState to, String reason) {

    boolean valid() {
        return switch (from) {
            case INIT -> to == TradeState.RESERVED || to == TradeState.CLOSED;
            case RESERVED -> to == TradeState.PAID || to == TradeState.CLOSED;
            case PAID, CLOSED -> false;
        };
    }
}
```

状态机的价值是防止非法跳转。生产事故中很多错误不是技术异常，而是状态被重复推进、逆向推进或越级推进。

## 深度增强：生产边界

最终一致不是“最终随便一致”。每个异步环节都要有唯一业务键、幂等处理、重试策略、死信、补偿任务和对账报表。
涉及资金和库存时，宁可慢一点，也要保证事实可追踪、可审计、可修复。

## 深度增强：面试高分表达

我会先承认分布式系统无法用一个本地事务覆盖所有服务，再说明如何把不确定性收敛：
本地事务写事实和 Outbox，消费者幂等处理，失败进入重试和死信，后台对账发现差异并补偿。

## 专家级完整回答

```text
同一订单事件顺序应从 MQ 路由和业务状态机两层保证。生产者用 orderId 作为消息 key，
让同一订单进入同一 partition，消费者按 partition 顺序处理。

同时消费者要做幂等、事件版本校验和状态机校验，防止重试、重放或旧事件造成状态回退。
```

## 回答评分点

高分答案应该覆盖：

- 使用业务 key 路由到同一 partition。
- 单 partition 才能保证顺序。
- 消费者侧仍要状态机校验。
- 事件版本可防止旧事件覆盖。
- 死信重放可能破坏顺序。

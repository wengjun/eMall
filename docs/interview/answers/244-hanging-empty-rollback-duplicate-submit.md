# 244 如何处理悬挂、空回滚和重复提交？

[返回按分类学习面试题](../README.md)

## 题目

如何处理悬挂、空回滚和重复提交？

## 先给面试官的短答案

悬挂、空回滚和重复提交通常出现在 TCC 或补偿事务里。
解决核心是为每个业务事务记录 Try、Confirm、Cancel 状态，并让 Confirm 和 Cancel 都幂等且可识别当前事务阶段。

不要假设 Try 一定先于 Cancel，也不要假设 Confirm 只会来一次。

## 三个概念

空回滚：

- Try 没执行成功，Cancel 先到了。

悬挂：

- Cancel 已执行，之后迟到的 Try 又来了。

重复提交：

- Confirm 被重复调用，导致资源重复扣减。

这些都是网络乱序和重试导致的正常异常。

## 处理方式

需要：

- 事务记录表。
- 全局事务 ID。
- 分支事务状态。
- Try 幂等。
- Confirm 幂等。
- Cancel 幂等。
- Cancel 后拒绝迟到 Try。

状态记录是判断当前动作是否合法的依据。

## 状态示例

状态可以是：

- TRYING。
- CONFIRMED。
- CANCELED。

如果收到 Cancel 且没有 Try 记录，可以插入 CANCELED 记录，表示空回滚已处理。

之后迟到 Try 看到 CANCELED，就直接拒绝，避免悬挂。

## 在 eMall 项目中怎么讲？

库存 TCC 中，Cancel 可能先于 Try 到达。

库存服务应记录该事务已取消，后续迟到的 Try 不能再预占库存；Confirm 重复到达时也不能重复扣减。

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
悬挂、空回滚和重复提交要靠事务状态表和幂等处理。每个 TCC 分支用全局事务 ID 记录 Try、Confirm、Cancel 状态。
Cancel 先到时记录空回滚；Try 迟到看到已取消就拒绝；Confirm 重复到达时按已确认幂等返回。

本质是不能依赖调用顺序，要用持久化状态判断每个动作是否合法。
```

## 回答评分点

高分答案应该覆盖：

- 知道空回滚、悬挂和重复提交含义。
- 需要全局事务 ID。
- 需要事务状态表。
- Try、Confirm、Cancel 都要幂等。
- Cancel 后迟到 Try 必须被拒绝。

# 214 3PC 解决了什么，又有什么限制？

[返回按分类学习面试题](../README.md)

## 题目

3PC 解决了什么，又有什么限制？

## 先给面试官的短答案

3PC 在 2PC 基础上增加了预提交阶段，并引入超时机制，目标是降低阻塞风险。
但它仍然依赖网络假设，无法彻底解决网络分区和一致性问题，在工程实践中并不常作为大型互联网交易的主方案。

它是理解分布式提交协议的重要理论，但不是万能生产答案。

## 3PC 三个阶段

三个阶段：

- CanCommit：询问参与者是否可以提交。
- PreCommit：通知参与者进入预提交。
- DoCommit：最终提交。

相比 2PC，3PC 把准备和提交之间拆得更细。

## 解决的问题

3PC 想解决：

- 减少参与者长期阻塞。
- 让参与者在超时后有机会继续推进。
- 降低协调者故障带来的不确定性。

它通过超时和中间阶段减少某些阻塞场景。

## 限制

限制包括：

- 网络分区下仍可能不一致。
- 协议更复杂。
- 通信轮次更多。
- 性能成本更高。
- 对时钟和超时假设敏感。

现实系统更常选择最终一致、共识协议或业务补偿方案。

## 在 eMall 项目中怎么讲？

eMall 下单不会因为 3PC 多一个阶段就适合用全局提交。

订单、库存、支付的高并发链路仍应优先使用本地事务、可靠消息、状态机、补偿和对账。

## 深度增强：可观测与配置治理图

![指标、日志、Trace 和告警平台](../assets/observability-platform.svg)

配置、日志、指标和 Trace 不是附属能力，而是生产系统定位问题和控制变更风险的基础。
没有可观测性，限流、熔断、回滚和补偿都很难判断是否有效。

## 深度增强：Java 17 观测信号示例

```java
import java.time.Instant;
import java.util.Map;

record ObservabilityEvent(
        Instant time,
        String traceId,
        String service,
        String eventType,
        Map<String, String> tags) {
}

final class TraceTagPolicy {

    boolean shouldKeep(String key) {
        return !key.equalsIgnoreCase("password")
                && !key.equalsIgnoreCase("secret")
                && !key.equalsIgnoreCase("token");
    }
}
```

这段代码体现生产观测的两个重点：所有关键事件要能关联 traceId，敏感信息不能进入日志和标签。

## 深度增强：生产边界

日志越多不代表越好。核心链路要控制日志成本、采样率、脱敏和索引字段。告警也不能只看机器指标，
还要看下单成功率、支付成功率、库存失败率、Outbox 积压和用户投诉。

## 深度增强：面试高分表达

我会把可观测性讲成故障闭环：指标发现异常，Trace 定位慢在哪里，日志解释发生了什么，
告警和 Runbook 指导恢复。配置变更也要有版本、审批、灰度、审计和回滚，避免配置事故变成全站事故。

## 专家级完整回答

```text
3PC 在 2PC 之上增加 CanCommit、PreCommit 和 DoCommit，通过预提交阶段和超时机制降低阻塞概率。
但它仍依赖网络和超时假设，网络分区下仍可能产生不一致，而且通信成本更高。

所以 3PC 更多是理论上的改进。大型互联网交易系统通常不会把它作为核心方案，而是采用最终一致和业务补偿。
```

## 回答评分点

高分答案应该覆盖：

- 3PC 包含 CanCommit、PreCommit、DoCommit。
- 它试图降低 2PC 阻塞。
- 网络分区下仍有限制。
- 通信成本更高。
- 生产交易系统更常用业务最终一致方案。

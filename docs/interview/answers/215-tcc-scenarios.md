# 215 TCC 适合哪些场景？

[返回按分类学习面试题](../README.md)

## 题目

TCC 适合哪些场景？

## 先给面试官的短答案

TCC 适合需要先预留资源，再确认或取消的强业务一致场景，比如库存预占、账户冻结、优惠券锁定。
它把业务操作拆成 Try、Confirm、Cancel 三个阶段，每个阶段都要由业务自己实现。

TCC 一致性强，但开发成本高，必须处理幂等、空回滚、悬挂和重复提交。

## 三个阶段

阶段如下：

- Try：检查并预留资源。
- Confirm：确认使用资源。
- Cancel：取消并释放资源。

Try 不能只做检查，它通常要实际冻结或预占资源，否则 Confirm 时资源可能已经不存在。

## 适合场景

适合：

- 库存预占。
- 账户余额冻结。
- 优惠券锁定。
- 名额预约。
- 需要明确释放资源的交易。

这些场景都有“先占住，再确认或释放”的业务语义。

## 不适合场景

不适合：

- 无法预留资源的操作。
- Confirm 和 Cancel 难以实现的操作。
- 参与方很多的长流程。
- 对吞吐要求极高但一致性要求没那么强的异步流程。

TCC 对业务侵入较强，不应滥用。

## 在 eMall 项目中怎么讲？

库存服务可以实现 TCC：

- Try：预占库存。
- Confirm：确认扣减。
- Cancel：释放预占。

订单取消或支付超时时调用 Cancel，支付成功后调用 Confirm。

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
TCC 适合具有资源预留语义的场景。Try 阶段检查并冻结资源，Confirm 阶段确认消费资源，Cancel 阶段释放资源。
典型例子是库存预占、余额冻结和优惠券锁定。

TCC 的难点是业务侵入和异常处理，每个阶段都要幂等，并处理空回滚、悬挂、重复提交和补偿失败。
```

## 回答评分点

高分答案应该覆盖：

- TCC 是 Try、Confirm、Cancel。
- Try 要预留资源。
- 适合库存、余额、优惠券等资源型场景。
- 开发成本和业务侵入高。
- 必须处理幂等、空回滚和悬挂。

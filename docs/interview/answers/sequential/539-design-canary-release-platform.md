# 539 设计灰度发布平台

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

设计灰度发布平台。

## 先给面试官的短答案

灰度发布平台负责把新版本按小流量、指定用户、租户、地区或标签逐步放量，并基于指标自动判断是否继续、暂停或回滚。
核心能力包括版本管理、流量规则、发布批次、指标门禁、自动回滚、审批、审计和数据库兼容检查。

## 核心流程

先部署新版本但不接流量。然后选择灰度对象，例如内部用户、员工、指定商家、1% 流量或某个地区。

平台下发路由规则到网关、Service Mesh 或服务治理层。每一批放量后观察错误率、P99、订单成功率、支付成功率和业务指标。

指标稳定后继续放量，否则暂停或回滚。

## 发布策略

支持按比例、按用户 ID、按租户、按 header、按 app 版本、按地区和按商家灰度。

核心交易服务要小步放量，非核心服务可以更快。高风险变更需要审批和人工确认。

## 自动门禁

发布平台应内置 SLO 和业务指标门禁。比如新版本订单成功率下降、支付成功率下降、库存失败率升高或 P99 超阈值，
自动暂停放量并通知负责人。

回滚前要检查数据库和消息兼容性，避免代码回滚后旧版本无法读取新数据。

## 在 eMall 项目中怎么讲？

eMall 的 `release` 模块负责发布策略，`traffic` 控制灰度流量，`analytics` 和 `operations` 提供指标和门禁。
`order`、`payment`、`inventory` 的发布必须绑定业务指标观察。

## 深度增强：灰度发布闭环图

![配置中心和灰度发布闭环](../../assets/config-release-loop.svg)

灰度发布平台的核心是控制爆炸半径。它不是“按 1%、10%、50% 放量”这么简单，
而是每个批次都要有技术指标、业务指标和回滚条件。

## 深度增强：Java 17 发布批次模型

```java
public enum ReleaseDecision {
    CONTINUE,
    PAUSE,
    ROLLBACK
}

public record CanaryBatch(
        String releaseId,
        int batchNo,
        int trafficPercentage,
        String targetExpression,
        Instant startedAt) {
}

public record CanaryMetrics(
        double errorRate,
        long p99LatencyMillis,
        double orderSuccessRate,
        double paymentSuccessRate) {
}
```

门禁逻辑可以明确写出来：

```java
public final class CanaryGate {

    public ReleaseDecision evaluate(CanaryMetrics metrics) {
        if (metrics.errorRate() > 0.01) {
            return ReleaseDecision.ROLLBACK;
        }
        if (metrics.p99LatencyMillis() > 500) {
            return ReleaseDecision.PAUSE;
        }
        if (metrics.orderSuccessRate() < 0.995 || metrics.paymentSuccessRate() < 0.995) {
            return ReleaseDecision.PAUSE;
        }
        return ReleaseDecision.CONTINUE;
    }
}
```

## 深度增强：生产边界

- 灰度对象可以是员工、指定商家、地区、用户 ID、App 版本或流量比例。
- 核心交易服务必须绑定业务指标，不只看 CPU 和错误率。
- 回滚前要确认数据库、消息和配置是否向后兼容。
- 灰度平台要记录每次放量、暂停、回滚的操作人和原因。
- 自动回滚要有保护，避免监控误报导致频繁抖动。

## 深度增强：面试高分表达

```text
灰度发布的价值是用小流量暴露问题，并用指标决定是否继续。我的平台会按用户、租户、地区、header 或比例放量，
每批观察错误率、P99、下单成功率、支付成功率和核心依赖指标。指标恶化时自动暂停或回滚，
但回滚前要检查数据库和消息兼容性。
```

## 专家级完整回答

```text
灰度发布平台的目标是控制变更爆炸半径，并用数据判断是否继续放量。

我会支持按用户、租户、地区、header 和比例灰度。每个批次都要观察技术指标和业务指标，
例如错误率、P99、下单成功率和支付成功率。

平台要能自动暂停和回滚，但回滚前要检查数据库兼容。成熟的灰度不是只会切流量，
而是把流量、指标、门禁和回滚预案串成闭环。
```

## 回答评分点

高分答案应该覆盖：

- 覆盖版本、流量规则、批次、审批、审计和回滚。
- 能说明按用户、租户、地区、比例等灰度方式。
- 强调技术指标和业务指标双门禁。
- 知道自动暂停和回滚。
- 能指出数据库兼容影响回滚。

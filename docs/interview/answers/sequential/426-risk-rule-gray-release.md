# 426 风控规则如何灰度上线？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

风控规则如何灰度上线？

## 先给面试官的短答案

风控规则灰度上线要先影子模式观察命中效果，再小流量灰度，逐步扩大范围，并持续监控误杀率、
拦截率、转化率、投诉率和人工审核结果。

高风险规则不能直接全量拦截，否则可能误伤正常交易。

## 发布阶段

阶段：

- 离线回放历史数据。
- 影子模式只记录不拦截。
- 小比例灰度。
- 指定城市、渠道或用户群灰度。
- 扩大流量。
- 全量上线。

每一步都要可回滚。

## 监控指标

指标：

- 规则命中率。
- 拦截率。
- 误杀率。
- 人工审核通过率。
- 支付成功率。
- 下单转化率。
- 用户投诉率。

风控效果和业务影响都要看。

## 安全机制

机制：

- 规则版本管理。
- 审批发布。
- 一键回滚。
- 命中原因可解释。
- 白名单和豁免。
- 审计日志。

风控规则本身也是高危配置。

## 在 eMall 项目中怎么讲？

eMall 新增“高频领券账号拦截”规则时，先用历史领券数据回放，再在线影子模式记录命中但不拒绝。

确认误杀率可接受后，只对 1% 流量拦截，并监控领券成功率、投诉和人工审核结果。

## 深度增强：风控灰度闭环图

![风控实时决策和规则灰度闭环](../../assets/risk-decision-engine.svg)

风控规则发布要先证明“规则有效且误伤可控”，不能只看命中率。离线回放验证历史覆盖，
影子模式验证线上真实命中，小流量灰度验证处置动作对业务的影响，最后才考虑全量。

## 深度增强：Java 17 灰度规则决策示例

```java
import java.util.Map;
import java.util.Set;

enum RiskAction {
    ALLOW,
    CAPTCHA,
    REVIEW,
    REJECT
}

record RiskContext(
        long userId,
        String city,
        String channel,
        Map<String, Long> counters) {
}

record RiskRule(
        String ruleId,
        int version,
        int trafficPercent,
        Set<String> grayCities,
        boolean shadowOnly,
        long maxCouponClaimsPerHour,
        RiskAction action) {
}

final class RiskGrayEvaluator {

    RiskAction evaluate(RiskContext context, RiskRule rule) {
        if (!rule.grayCities().isEmpty() && !rule.grayCities().contains(context.city())) {
            return RiskAction.ALLOW;
        }
        if (Math.floorMod(Long.hashCode(context.userId()), 100) >= rule.trafficPercent()) {
            return RiskAction.ALLOW;
        }
        long claims = context.counters().getOrDefault("couponClaimsPerHour", 0L);
        boolean hit = claims > rule.maxCouponClaimsPerHour();
        if (!hit || rule.shadowOnly()) {
            return RiskAction.ALLOW;
        }
        return rule.action();
    }
}
```

这段代码体现灰度的两个关键点：第一，流量分桶要稳定，同一个用户不能每次请求随机变化；
第二，影子模式命中后仍放行，但必须记录命中原因、规则版本、特征快照和最终业务结果。

## 深度增强：生产边界

风控规则是高危配置，需要审批、版本、灰度、审计、一键回滚和兜底白名单。对于支付、退款、
大额优惠和商家处罚这类高影响动作，建议把 `REJECT` 拆成 `REVIEW` 过渡，避免误杀直接变成事故。

指标不能只看“拦了多少”。还要看投诉率、人工审核通过率、支付成功率、下单转化率、误杀率、
坏账或薅羊毛损失下降值。高分回答要能体现安全和业务之间的平衡。

## 深度增强：面试高分表达

我会把风控规则当成“可发布的软件”治理。上线前先离线回放，线上先影子观察，再按稳定分桶小流量灰度。
每个阶段都要看误杀和业务指标，并且能一键回滚到上一版本。这样既能提升拦截能力，
也不会因为一条错误规则影响核心交易。

## 专家级完整回答

```text
风控规则灰度要从离线回放、影子模式、小流量灰度到全量上线逐步推进。影子模式只记录命中，不影响
用户，用来评估误杀和覆盖。

上线过程中要监控命中率、误杀率、拦截率、支付成功率、转化率和投诉率，并支持审批、版本、审计和
一键回滚。风控规则不能无验证直接全量拦截。
```

## 回答评分点

高分答案应该覆盖：

- 离线回放和影子模式。
- 小流量灰度。
- 监控误杀率和业务指标。
- 规则版本和回滚。
- 高危规则需要审批。

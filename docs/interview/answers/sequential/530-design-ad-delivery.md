# 530 设计广告投放系统的核心链路

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

设计广告投放系统的核心链路。

## 先给面试官的短答案

广告投放核心链路包括广告请求、候选召回、定向过滤、预算校验、竞价排序、频控、创意渲染、曝光点击埋点和计费。
关键要求是低延迟、预算准确、频控有效、反作弊、可审计和实验可控。

## 核心流程

用户访问搜索页或详情页时，广告服务接收场景、用户、关键词、类目和设备信息。

系统召回候选广告，再根据地域、用户标签、类目、关键词、商家、商品状态和审核状态过滤。

对剩余广告进行预算校验、出价和质量分排序，选择最终展示广告。曝光和点击通过埋点进入计费和效果分析。

## 预算和计费

预算控制要避免超花。可以使用 Redis 原子扣减日预算、活动预算和广告主预算，并异步落库。

计费要防重复，曝光和点击事件需要唯一 ID。点击计费要做反作弊和无效点击过滤。

## 频控和体验

频控限制同一用户、设备或 IP 在一定时间窗口看到同一广告的次数，避免用户体验下降和广告浪费。

广告必须过滤下架商品、违规商品和预算耗尽广告。广告系统不能为了收入突破平台规则。

## 在 eMall 项目中怎么讲？

eMall 的 `advertising` 模块负责广告投放，`risk` 做反作弊，`experiment` 做投放实验，`analytics` 做效果分析，
`product` 和 `merchant` 提供商品与商家审核状态。

## 深度增强：广告在线链路图

![推荐和广告在线决策链路](../../assets/recommendation-ads-online.svg)

广告和推荐的链路相似，都要低延迟完成召回、过滤、排序和返回。但广告多了商业约束：
预算不能超花，频控不能失效，计费要幂等，反作弊要过滤无效曝光和点击，审核不通过不能投放。

## 深度增强：Java 17 预算和频控示例

```java
import java.math.BigDecimal;
import java.util.List;

record AdCandidate(
        long adId,
        long advertiserId,
        BigDecimal bid,
        double qualityScore,
        boolean approved,
        boolean productAvailable) {
}

interface BudgetService {
    boolean tryReserve(long adId, BigDecimal maxCost);
}

interface FrequencyCapService {
    boolean allow(long userId, long adId);
}

final class AdSelector {

    AdCandidate select(long userId, List<AdCandidate> candidates,
            BudgetService budgetService, FrequencyCapService frequencyCapService) {
        return candidates.stream()
                .filter(AdCandidate::approved)
                .filter(AdCandidate::productAvailable)
                .filter(ad -> frequencyCapService.allow(userId, ad.adId()))
                .sorted((left, right) -> Double.compare(score(right), score(left)))
                .filter(ad -> budgetService.tryReserve(ad.adId(), ad.bid()))
                .findFirst()
                .orElse(null);
    }

    private double score(AdCandidate ad) {
        return ad.bid().doubleValue() * ad.qualityScore();
    }
}
```

预算预占要使用原子操作，常见实现是 Redis Lua、数据库条件更新或预算服务串行化分片。
如果只是先查余额再扣减，在高并发广告请求下很容易超花。

## 深度增强：生产边界

广告计费要有唯一事件 ID，曝光、点击、转化都要幂等。点击计费还要通过风控过滤机器人、
异常 IP、异常设备、短时间重复点击和商家自点。计费和报表通常允许最终一致，但资金结算要可对账。

广告系统不能只追求收入。下架商品、违规创意、审核未通过、预算耗尽和频控超限都必须过滤。
如果投放链路失败，可以不展示广告或展示自然推荐，不能为了填充率突破平台治理规则。

## 深度增强：面试高分表达

我会把广告投放讲成“推荐链路加商业约束和资金闭环”。召回、定向、过滤、竞价排序负责展示效果，
预算、频控、计费幂等和反作弊负责商业正确性。这样能说明我既懂在线低延迟系统，
也懂广告系统特有的预算、审计和结算风险。

## 专家级完整回答

```text
广告投放系统的核心是低延迟决策和准确计费。

我会把链路拆成候选召回、定向过滤、预算校验、竞价排序、频控、创意返回和曝光点击计费。
预算扣减和计费必须幂等，曝光点击事件要可追踪，反作弊要过滤无效流量。

广告系统还要和平台治理协同。下架、违规、审核未通过或预算耗尽的广告不能被投放，
否则会影响用户体验和商家结算。
```

## 回答评分点

高分答案应该覆盖：

- 覆盖召回、定向、预算、竞价、频控和计费。
- 能说明预算防超花和事件幂等。
- 知道曝光、点击埋点和反作弊。
- 强调商品审核、下架和违规过滤。
- 能结合广告效果指标和实验说明。
## 深度完善：专项验收清单

围绕「设计广告投放系统的核心链路」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
回答时要把概念、代码、数据、失败路径和指标串起来，证明自己不是只理解单点知识。

### 项目落点

- 先说明它在 eMall 哪个模块或链路中出现，例如交易、库存、支付、搜索、风控、发布或可观测性。
- 再说明它保护的核心目标：正确性、可用性、延迟、成本、安全或协作效率。
- 最后补失败场景：超时、重试、重复请求、状态不一致、热点流量、配置错误或发布回滚。

### 验收证据

- 代码证据：关键类、状态机、唯一约束、事务边界、线程池隔离或配置项。
- 测试证据：单元测试、集成测试、契约测试、压测、故障注入或回归用例。
- 运行证据：指标看板、Trace、结构化日志、告警、Runbook、对账结果或补偿记录。

### 高分收束

面试最后要回到取舍：当前方案为什么足够简单可靠，什么时候需要升级，升级时如何灰度、回滚和验证。
这样回答能体现生产系统判断力，而不是只罗列技术名词。

深度完善标记：专题增强答案已补项目落点、验收证据和取舍收束。

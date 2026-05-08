# 537 设计限流系统

[返回按分类学习面试题](../README.md)

## 题目

设计限流系统。

## 先给面试官的短答案

限流系统用于保护服务不被突发流量、恶意请求或下游容量不足拖垮。它要支持网关限流、服务限流、用户限流、
租户限流、接口限流和热点资源限流，算法可选固定窗口、滑动窗口、令牌桶和漏桶。

## 限流维度

常见维度包括 IP、用户、设备、appKey、商家、租户、接口、SKU、活动和下游资源。

电商系统不能只做全局限流。比如秒杀热点 SKU 应按 SKU 限流，开放平台应按 appKey 和商家限流，
支付通道应按渠道容量限流。

## 算法选择

固定窗口实现简单，但窗口边界可能出现突刺。滑动窗口更平滑，但存储和计算成本更高。

令牌桶允许一定突发，适合接口流量控制。漏桶输出平滑，适合保护下游。

分布式限流常用 Redis Lua 保证原子性，本地限流适合低延迟和实例级保护。

## 策略治理

限流规则要支持配置中心动态发布、灰度、版本、审计和回滚。限流响应要有明确错误码和重试建议。

限流不是越严格越好。要结合业务优先级，高价值交易用户、普通浏览、爬虫和后台任务可以有不同策略。

## 在 eMall 项目中怎么讲？

eMall 的 `traffic` 模块可以承载限流策略，`gateway` 做入口限流，业务服务做资源级限流。
秒杀、开放平台、支付通道和热点 SKU 是重点限流场景。

## 深度增强：平台架构图

![限流、熔断、降级治理平台](../assets/governance-platform.svg)

限流系统不是一个算法类，而是一个治理平台。真正生产级能力包括规则管理、灰度发布、审计、回滚、指标和误伤分析。
运行时 SDK 要能本地缓存规则，避免配置中心故障时所有请求链路不可用。

## 深度增强：Java 17 规则模型

限流规则要能表达资源、维度、算法、阈值和优先级：

```java
public enum RateLimitAlgorithm {
    TOKEN_BUCKET,
    LEAKY_BUCKET,
    SLIDING_WINDOW
}

public record RateLimitRule(
        String ruleId,
        String resource,
        String dimension,
        RateLimitAlgorithm algorithm,
        long limit,
        Duration window,
        int priority,
        boolean enabled) {
}

public record RateLimitRequest(
        String resource,
        Map<String, String> attributes) {
}

public record RateLimitDecision(boolean allowed, String reason, Duration retryAfter) {
}
```

运行时根据规则和请求属性做决策：

```java
public final class RateLimitEngine {

    private final RuleRepository ruleRepository;
    private final Map<String, RateLimiter> limiters;

    public RateLimitEngine(
            RuleRepository ruleRepository,
            Map<String, RateLimiter> limiters) {
        this.ruleRepository = ruleRepository;
        this.limiters = limiters;
    }

    public RateLimitDecision decide(RateLimitRequest request) {
        List<RateLimitRule> rules = ruleRepository.findEnabledRules(request.resource());
        for (RateLimitRule rule : rules) {
            String key = buildKey(rule, request.attributes());
            RateLimiter limiter = limiters.get(rule.ruleId());
            if (!limiter.tryAcquire(key, rule.limit(), rule.window())) {
                return new RateLimitDecision(false, rule.ruleId(), rule.window());
            }
        }
        return new RateLimitDecision(true, "allowed", Duration.ZERO);
    }
}
```

## 深度增强：生产边界

- 规则要灰度，避免一次错误配置误伤全站。
- 限流指标要区分规则、接口、用户类型、租户和资源。
- 系统要支持白名单和高优先级流量，但必须可审计。
- 限流不是越严越好，要关注转化率、订单成功率和用户投诉。
- 配置中心故障时，运行时要使用最后一次有效规则。

## 深度增强：面试高分表达

```text
我会把限流系统分成控制面和运行面。控制面负责规则配置、审批、灰度、审计和回滚；
运行面在网关和服务 SDK 中执行规则，并本地缓存策略。限流维度不能只有全局 QPS，
还要支持用户、IP、appKey、SKU、商家、活动和下游通道。指标上要能看到谁被限流、为什么被限流。
```

## 专家级完整回答

```text
限流系统的目标是把不可承受的流量挡在系统边界或资源边界之外。

我会按维度设计限流：入口按 IP、用户和 appKey，业务按接口和租户，资源按 SKU、活动和下游通道。
算法上根据场景选择令牌桶、漏桶或滑动窗口。

生产限流还要有策略治理。规则要能灰度、回滚和审计，指标要显示被限流量和原因。
否则限流很容易从保护系统变成误伤用户。
```

## 回答评分点

高分答案应该覆盖：

- 覆盖网关、服务、用户、租户、接口和资源限流。
- 能比较固定窗口、滑动窗口、令牌桶和漏桶。
- 知道 Redis Lua 和本地限流的适用场景。
- 强调规则动态发布、灰度、审计和回滚。
- 能结合秒杀、开放平台、支付通道说明。
## 深度完善：专项验收清单

围绕「设计限流系统」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

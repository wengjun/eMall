# 538 设计熔断降级平台

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

设计熔断降级平台。

## 先给面试官的短答案

熔断降级平台用于在下游失败、慢调用或资源耗尽时隔离故障，并提供安全兜底。它要支持熔断规则、超时、重试、
半开探测、降级策略、动态配置、指标采集、告警、灰度和自动恢复。

## 核心能力

熔断能力包括按错误率、慢调用比例、异常数量和并发数打开断路器。断路器有关闭、打开和半开状态。

降级能力包括返回缓存、默认值、热门数据、只读模式、排队稍后处理或明确失败。

平台还要管理超时和重试。重试必须有上限和退避，不能在下游故障时放大流量。

## 策略设计

每个依赖应配置独立策略，例如支付通道、库存服务、推荐服务、搜索服务。不同依赖的降级动作不同。

推荐失败可以返回热门商品；库存失败不能默认有库存；支付确认失败可以进入待确认而不是直接成功。

## 自动恢复

下游恢复后不能立即全量放开。半开状态只放少量探测请求，成功后逐步恢复，失败则继续打开。

平台要观察恢复期错误率和延迟，避免下游刚恢复又被积压流量打挂。

## 在 eMall 项目中怎么讲？

eMall 的 `reliability` 和 `governance` 模块可以提供熔断降级策略，`traffic` 控制流量，`operations` 提供策略管理和告警。
订单、支付、库存要有不同的降级边界。

## 深度增强：平台架构图

![限流、熔断、降级治理平台](../../assets/governance-platform.svg)

熔断降级平台要特别强调“业务安全”。推荐服务可以降级成热门推荐，库存服务不能降级成“默认有库存”，
支付确认不能降级成“默认支付成功”。不同依赖要有不同的兜底策略。

## 深度增强：Java 17 策略模型

```java
public enum FallbackType {
    FAIL_FAST,
    CACHE,
    DEFAULT_VALUE,
    QUEUE_FOR_RETRY,
    MANUAL_REVIEW
}

public record ResilienceRule(
        String dependency,
        Duration timeout,
        double errorRateThreshold,
        double slowCallRateThreshold,
        Duration openDuration,
        FallbackType fallbackType,
        boolean enabled) {
}

public record DependencyCall<T>(String dependency, Supplier<T> supplier) {
}
```

运行时根据规则包装下游调用：

```java
public final class ResilienceExecutor {

    private final ResilienceRuleRepository ruleRepository;
    private final CircuitBreakerRegistry breakerRegistry;
    private final FallbackHandler fallbackHandler;

    public ResilienceExecutor(
            ResilienceRuleRepository ruleRepository,
            CircuitBreakerRegistry breakerRegistry,
            FallbackHandler fallbackHandler) {
        this.ruleRepository = ruleRepository;
        this.breakerRegistry = breakerRegistry;
        this.fallbackHandler = fallbackHandler;
    }

    public <T> T execute(DependencyCall<T> call) {
        ResilienceRule rule = ruleRepository.getRule(call.dependency());
        CircuitBreaker breaker = breakerRegistry.get(call.dependency());
        if (!breaker.allowRequest(Instant.now())) {
            return fallbackHandler.fallback(call.dependency(), rule.fallbackType());
        }
        try {
            T result = call.supplier().get();
            breaker.recordSuccess();
            return result;
        } catch (RuntimeException ex) {
            breaker.recordFailure(Instant.now());
            return fallbackHandler.fallback(call.dependency(), rule.fallbackType());
        }
    }
}
```

## 深度增强：生产边界

- 超时是熔断的前提，没有超时就可能无限等待。
- 重试必须有上限、退避和 jitter，否则会放大故障。
- 降级结果必须符合业务安全，不能隐藏资金和库存错误。
- 半开探测要小流量，恢复要逐步放量。
- 规则变更要审计，生产手动强制降级要有审批和过期时间。

## 深度增强：面试高分表达

```text
我会把平台拆成控制面、运行时 SDK 和观测面。控制面下发每个依赖的超时、熔断阈值和降级动作；
运行时负责执行状态机、本地缓存规则和兜底；观测面关注错误率、慢调用、熔断次数和降级命中率。
最重要的是降级要按业务安全设计，库存和支付不能被错误地降级成成功。
```

## 专家级完整回答

```text
熔断降级平台的核心是故障隔离和业务安全兜底。

我会为每个下游依赖配置超时、错误率阈值、慢调用阈值、半开探测和降级动作。
降级动作必须按业务安全设计，不能把支付或库存失败降级成成功。

恢复也要平滑。半开探测和逐步放量能避免下游刚恢复就被打挂。
平台还要提供指标、告警、审计和动态规则发布。
```

## 回答评分点

高分答案应该覆盖：

- 覆盖熔断状态机、超时、重试、降级和恢复。
- 能说明不同业务有不同降级动作。
- 知道半开探测和平滑恢复。
- 强调重试不能放大故障。
- 能结合支付、库存、推荐等场景。
## 深度完善：专项验收清单

围绕「设计熔断降级平台」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

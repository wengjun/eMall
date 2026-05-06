# 379 重试 topic 和死信 topic 如何设计？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

重试 topic 和死信 topic 如何设计？

## 先给面试官的短答案

重试 Topic 用于处理临时失败的消息，死信 Topic 用于保存多次重试仍失败或不可恢复的消息。
设计时要包含重试次数、下一次重试时间、错误原因、原始 Topic、原始 offset 和 trace ID。

目标是隔离失败消息，避免它阻塞正常消费。

## 重试 Topic

适合：

- 下游临时不可用。
- 网络超时。
- 数据库短暂异常。
- 限流导致处理失败。

重试要有退避策略，不能立即无限重试。

## 死信 Topic

适合：

- 消息格式错误。
- 业务数据非法。
- 多次重试失败。
- 无法反序列化。
- 依赖数据长期不存在。

死信消息要告警和人工或自动修复。

## 设计字段

字段包括：

- 原始 Topic。
- 原始 Partition。
- 原始 offset。
- 原始 key。
- 事件 ID。
- 重试次数。
- 错误码和错误信息。
- 下一次重试时间。
- trace ID。

这些字段支持排查和回放。

## 在 eMall 项目中怎么讲？

eMall 履约消费者调用仓储失败，可以把消息投递到 `fulfillment-retry-5m`，稍后再处理。

如果商品数据缺失或消息 schema 不兼容，重试多次仍失败后进入 `fulfillment-dlq`，告警给值班人员，
修复后再通过工具回放。

## 深度增强：重试和死信链路图

![Kafka 重试 Topic 和死信 Topic](../../assets/kafka-retry-dlq.svg)

重试 Topic 和死信 Topic 的核心价值是隔离失败消息。主消费链路不能因为一条坏消息阻塞整个分区，
否则一个数据问题会演变成全链路积压。

## 深度增强：Java 17 失败信封模型

```java
public record FailedMessageEnvelope(
        String originalTopic,
        int originalPartition,
        long originalOffset,
        String originalKey,
        String eventId,
        int retryCount,
        Instant nextRetryAt,
        String errorCode,
        String errorMessage,
        String traceId,
        String payload) {
}
```

消费者失败时按错误类型决定重试还是死信：

```java
public final class RetryDecisionPolicy {

    private static final int MAX_RETRY = 5;

    public RetryDecision decide(FailedMessageEnvelope message, Throwable error) {
        if (error instanceof SchemaMismatchException) {
            return RetryDecision.deadLetter("SCHEMA_MISMATCH");
        }
        if (message.retryCount() >= MAX_RETRY) {
            return RetryDecision.deadLetter("RETRY_EXHAUSTED");
        }
        Duration delay = backoff(message.retryCount());
        return RetryDecision.retryAfter(delay);
    }

    private Duration backoff(int retryCount) {
        long seconds = Math.min(300, 1L << retryCount);
        long jitter = ThreadLocalRandom.current().nextLong(0, 5);
        return Duration.ofSeconds(seconds + jitter);
    }
}
```

## 深度增强：生产边界

- 可恢复错误才重试，不可恢复错误要尽快进死信。
- 重试必须退避，不能失败后立即打回主链路。
- 死信必须告警、可搜索、可修复、可回放。
- 回放要限速，并保留操作人、原因和 trace。
- 消费者业务逻辑仍要幂等，重试 Topic 不能替代幂等设计。

## 深度增强：面试高分表达

```text
我会把失败消息从主链路隔离出来。短暂下游失败进入 retry topic，并带 retryCount、nextRetryAt 和 traceId；
格式错误、业务数据非法或超过重试上限进入 DLQ。DLQ 不是垃圾桶，而是一个可告警、可修复、可审计、
可限速回放的运维闭环。
```

## 专家级完整回答

```text
重试 Topic 处理可恢复失败，死信 Topic 保存不可恢复或超过重试上限的消息。重试要有退避和次数
上限，死信要保留原始位置、错误原因、事件 ID 和 trace ID。

它们的核心价值是把异常消息从主消费链路隔离出来，避免阻塞正常消息，同时保留排查、修复和回放
能力。
```

## 回答评分点

高分答案应该覆盖：

- 重试处理临时失败。
- 死信处理不可恢复失败。
- 重试要退避和上限。
- 死信保留原始上下文。
- 异常消息不能阻塞主链路。
## 深度完善：专项验收清单

围绕「重试 topic 和死信 topic 如何设计？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

# 368 Kafka 事务适合什么场景？

[返回按分类学习面试题](../README.md)

## 题目

Kafka 事务适合什么场景？

## 先给面试官的短答案

Kafka 事务适合 Kafka 内部的读处理写场景，例如消费一个 Topic，处理后写入另一个 Topic，并把
offset 提交和输出消息作为一个事务。

它不适合直接解决数据库、本地事务和外部 HTTP 调用的端到端一致性问题。

## 适合场景

适合：

- Kafka Streams。
- 事件清洗后写新 Topic。
- 聚合结果写回 Kafka。
- 消费输入 Topic 后产生输出 Topic。
- 需要 offset 和输出消息原子提交。

这些场景的事务边界主要在 Kafka 内。

## 不适合场景

不适合单独解决：

- 数据库写入和 Kafka 发送原子一致。
- 支付接口调用和消息发送原子一致。
- Redis 更新和消息发送原子一致。
- 多个外部系统的全局事务。

这些需要 Outbox、事务消息、Saga 或补偿。

## 成本

成本包括：

- 延迟增加。
- 吞吐下降。
- 配置复杂。
- 运维排查复杂。
- 事务超时风险。

不是所有消息都需要 Kafka 事务。

## 在 eMall 项目中怎么讲？

eMall 数据平台消费订单事件后生成用户行为聚合事件，可以使用 Kafka 事务保证输入 offset 和输出
聚合事件一致提交。

但订单服务本地数据库写入和订单事件发布，更适合使用 Outbox，先在本地事务写订单和 Outbox 记录，
再异步可靠投递 Kafka。

## 深度增强：缓存和消息治理图

![数据库、缓存和消息一致性链路](../assets/data-cache-mq.svg)

缓存和消息题要关注一致性、削峰、延迟、积压和恢复。
Redis 很快，但会遇到穿透、击穿、雪崩、热点 key 和内存淘汰；
MQ 能解耦和削峰，但会带来重复消费、乱序、积压和死信处理。

## 深度增强：Java 17 幂等消费示例

```java
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class LocalIdempotentConsumer {
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    boolean tryHandle(String messageKey, Runnable handler) {
        if (!processedKeys.add(messageKey)) {
            return false;
        }
        handler.run();
        return true;
    }
}
```

这个示例只适合解释幂等思想。生产环境不能用本地内存做全局幂等，要使用数据库唯一键、Redis 原子操作或业务状态机。

## 深度增强：生产边界

缓存要有 TTL、容量、降级和回源保护；消息要有重试、死信、延迟队列、消费幂等和积压告警。
缓存不一致要能修复，消息失败要能回放，不能只依赖人工查日志。

## 深度增强：面试高分表达

我会把缓存和消息都看成性能与稳定性工具，而不是正确性事实来源。
正确性由数据库事实、状态机、幂等和对账保证；缓存和 MQ 负责降低延迟、削峰填谷和解耦系统。

## 专家级完整回答

```text
Kafka 事务适合 Kafka 到 Kafka 的流处理场景，保证消费 offset 和输出消息在 Kafka 事务边界内
一致提交。它常见于 Kafka Streams 或事件清洗聚合。

它不能自动解决数据库、Redis、HTTP 下游和第三方支付的端到端一致性。电商核心交易通常用 Outbox、
幂等、状态机和补偿来保证业务一致性。
```

## 回答评分点

高分答案应该覆盖：

- Kafka 事务适合读处理写 Kafka。
- offset 和输出消息原子提交。
- 不覆盖外部数据库和接口。
- 有性能和复杂度成本。
- 订单事件发布更适合 Outbox。

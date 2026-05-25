# 385 MQ 故障时核心链路怎么办？

[返回按分类学习面试题](../README.md)

## 题目

MQ 故障时核心链路怎么办？

## 先给面试官的短答案

MQ 故障时核心链路不能简单失败，也不能假装消息已发送。生产级做法是把核心业务写入和 Outbox
记录放在本地事务中，MQ 恢复后由 Relay 补发。对于强依赖 MQ 的非核心功能，可以降级或暂停。

核心目标是保证业务主数据正确，并保留事件补发能力。

## 直接发送 MQ 的问题

如果业务提交后直接发 MQ：

- MQ 故障会导致事件丢失。
- 回滚业务会影响用户体验。
- 重试可能阻塞请求。
- 不易判断是否发送成功。

核心链路需要更可靠的事件发布模式。

## Outbox 方案

流程：

- 本地事务写业务表。
- 同事务写 Outbox 表。
- 请求返回成功。
- Relay 异步扫描 Outbox。
- MQ 恢复后继续投递。
- 投递成功后标记 Outbox 成功。

这样 MQ 短暂故障不影响主交易提交。

## 降级策略

策略：

- 核心订单落库优先。
- 非核心消息暂停发送。
- Relay 堆积告警。
- 限制新流量防止 Outbox 爆仓。
- MQ 恢复后限速补发。
- 消费者幂等处理重复事件。

恢复时要防止补发流量打垮下游。

## 在 eMall 项目中怎么讲？

eMall 下单成功时，订单表和 Outbox 表同事务提交。即使 Kafka 临时不可用，订单不会丢。

Kafka 恢复后，Outbox Relay 把 `order-created` 事件补发给库存、履约和搜索消费者。消费者通过
事件 ID 幂等处理。

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
MQ 故障时核心链路要保证主数据正确，并保留事件补发能力。最常用的是 Outbox：业务表和 Outbox
记录在本地事务中提交，Relay 异步投递 MQ。

这样 MQ 短暂不可用不会导致订单落库失败，也不会丢事件。恢复后要限速补发，并依赖消费者幂等
处理可能的重复消息。
```

## 回答评分点

高分答案应该覆盖：

- MQ 故障不能静默丢事件。
- 核心主数据优先正确。
- Outbox 本地事务。
- MQ 恢复后补发。
- 恢复补发要限速和幂等。

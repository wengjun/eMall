# 384 Kafka 和 RabbitMQ 如何取舍？

[返回按分类学习面试题](../README.md)

## 题目

Kafka 和 RabbitMQ 如何取舍？

## 先给面试官的短答案

Kafka 更适合高吞吐、事件流、日志、数据管道、可回放和多订阅者场景。RabbitMQ 更适合传统任务队列、
复杂路由、低延迟命令消息和较灵活的确认路由模型。

电商核心事件总线和数据同步更偏 Kafka，复杂工作队列或小规模命令分发可以考虑 RabbitMQ。

## Kafka 适合

适合：

- 订单事件流。
- 用户行为日志。
- 搜索索引同步。
- 数据仓库同步。
- 多消费者订阅。
- 消息回放。
- 高吞吐写入。

Kafka 的优势是吞吐、持久日志和可回放。

## RabbitMQ 适合

适合：

- 任务分发。
- 复杂路由。
- RPC 风格异步命令。
- 延迟队列。
- 小规模低延迟消息。
- 需要灵活 exchange routing 的场景。

RabbitMQ 的路由模型更灵活。

## 取舍维度

维度：

- 吞吐量。
- 是否需要回放。
- 消息保留时间。
- 路由复杂度。
- 顺序性要求。
- 运维团队经验。
- 生态和监控能力。

不是哪个更高级，而是场景不同。

## 在 eMall 项目中怎么讲？

eMall 订单、支付、库存、搜索、数仓事件适合 Kafka，因为需要高吞吐、多订阅者和回放。

如果有后台异步任务分发、客服工单派发或小规模命令消息，可以评估 RabbitMQ，但不建议在核心
事件总线中混用太多消息技术。

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
Kafka 是分布式提交日志，更适合高吞吐事件流、多消费者订阅、持久保留和回放。RabbitMQ 是传统
消息代理，更适合任务队列、复杂路由和低延迟命令消息。

电商中订单事件、搜索同步、行为日志和数仓链路通常选 Kafka。是否选 RabbitMQ 要看路由复杂度、
吞吐要求、回放需求和团队运维能力。
```

## 回答评分点

高分答案应该覆盖：

- Kafka 适合高吞吐事件流。
- Kafka 支持保留和回放。
- RabbitMQ 路由和任务队列更灵活。
- 按场景取舍。
- 电商事件总线常用 Kafka。

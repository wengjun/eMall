# 367 Producer 幂等解决什么问题？

[返回按分类学习面试题](../README.md)

## 题目

Producer 幂等解决什么问题？

## 先给面试官的短答案

Kafka Producer 幂等主要解决生产者重试导致同一条消息在同一 Partition 内重复写入的问题。
开启幂等后，Kafka 通过 Producer ID 和序列号识别重复请求，避免重试造成日志重复。

它解决的是 Producer 到 Kafka 写入链路的重复，不等于业务消费幂等。

## 为什么会重复

典型流程：

- Producer 发送消息。
- Broker 实际写入成功。
- 响应在网络中丢失。
- Producer 认为失败并重试。
- 如果没有幂等，消息可能写入两次。

重试提高可靠性，也带来重复风险。

## 幂等机制

机制：

- Producer 获得 Producer ID。
- 每个 Partition 维护序列号。
- Broker 识别重复序列号。
- 重复消息不会再次追加。

该能力有明确边界。

## 边界

边界：

- 主要解决单 Producer 会话内写入重复。
- 不解决消费者重复处理。
- 不解决业务数据库重复写。
- 不解决多业务请求语义重复。
- 跨系统仍要业务幂等。

不能用 Kafka Producer 幂等替代订单幂等。

## 在 eMall 项目中怎么讲？

eMall 订单服务发布 `order-created` 事件时应开启 Producer 幂等，降低网络抖动重试导致 Kafka 内部
重复消息的概率。

但库存消费者仍要以 `order_no` 做幂等，因为重复消息、重放消息或人工补偿仍可能发生。

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
Producer 幂等解决的是生产者重试导致 Kafka 日志重复写入的问题。Broker 使用 Producer ID 和
Partition 内序列号识别重复请求。

它提升了 Producer 到 Kafka 链路的写入可靠性，但不保证消费者业务只执行一次。订单、库存和支付
仍要通过业务幂等、唯一约束和状态机处理重复。
```

## 回答评分点

高分答案应该覆盖：

- 重试可能造成重复写入。
- Producer ID 和序列号。
- 解决 Producer 到 Kafka 重复。
- 不解决消费端业务重复。
- 核心业务仍要幂等。

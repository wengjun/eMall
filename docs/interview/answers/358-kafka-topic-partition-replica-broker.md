# 358 Kafka 的 Topic、Partition、Replica、Broker 是什么？

[返回按分类学习面试题](../README.md)

## 题目

Kafka 的 Topic、Partition、Replica、Broker 是什么？

## 先给面试官的短答案

Topic 是消息的逻辑分类，Partition 是 Topic 的物理分片，Replica 是 Partition 的副本，Broker 是
Kafka 集群中的服务器节点。

Kafka 通过 Partition 实现并行读写和水平扩展，通过 Replica 实现高可用。

## Topic

Topic 表示一类消息。

例如：

- `order-created`。
- `payment-succeeded`。
- `inventory-deducted`。
- `product-changed`。

生产者向 Topic 写消息，消费者订阅 Topic。

## Partition

Partition 是 Topic 的分片。

特点：

- 一个 Topic 可以有多个 Partition。
- 每个 Partition 内消息有顺序。
- 不同 Partition 之间没有全局顺序。
- Partition 是并行消费的基础。

吞吐能力通常和 Partition 数量相关。

## Replica 和 Broker

Replica 是 Partition 的副本。

Broker 是 Kafka 服务器节点。一个 Partition 有一个 leader replica 和多个 follower replica。
生产和消费通常访问 leader，follower 从 leader 复制数据。

## 在 eMall 项目中怎么讲？

eMall 可以把订单创建事件写入 `order-created` Topic。该 Topic 按 `orderId` 或 `userId` 分配到
不同 Partition，提升并发处理能力。

每个 Partition 配置多个 Replica，某个 Broker 宕机后，其他副本可以选出新 leader 保持可用。

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
Kafka 中 Topic 是逻辑消息分类，Partition 是 Topic 的物理分片，Replica 是 Partition 的副本，
Broker 是集群节点。

Partition 提供并行读写和扩展能力，并保证单个 Partition 内有序。Replica 提供高可用，每个
Partition 有 leader 和 follower，读写通常经过 leader。
```

## 回答评分点

高分答案应该覆盖：

- Topic 是逻辑分类。
- Partition 是物理分片。
- Partition 内有序。
- Replica 提供高可用。
- Broker 是服务器节点。

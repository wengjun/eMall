# 370 为什么顺序通常只能保证同一个 partition 内？

[返回按分类学习面试题](../README.md)

## 题目

为什么顺序通常只能保证同一个 partition 内？

## 先给面试官的短答案

Kafka 的 Partition 是独立追加日志，每个 Partition 内有自己的 offset 顺序。不同 Partition 分布在
不同 Broker、由不同消费者并行处理，没有统一的全局时钟和全局追加点。

因此 Kafka 天然只能保证单 Partition 内顺序。

## 架构原因

原因：

- 每个 Partition 是独立日志。
- offset 只在 Partition 内递增。
- 不同 Partition 可并行写入。
- 不同 Partition 可由不同 Consumer 消费。
- Broker 不维护全局消息序列。

这是 Kafka 高吞吐设计的基础。

## 如果要求全局顺序

可以做到但代价很高：

- Topic 只用一个 Partition。
- 所有消息串行写入。
- 消费也基本串行。
- 吞吐和扩展性大幅下降。

大多数业务不需要全局顺序。

## 正确理解顺序

应按业务对象定义顺序：

- 同一订单状态有序。
- 同一支付单事件有序。
- 同一 SKU 库存事件有序。
- 同一用户行为相对有序。

局部顺序比全局顺序更实用。

## 在 eMall 项目中怎么讲？

eMall 不需要所有订单事件全局有序，只需要同一订单的状态事件有序。

因此使用 `orderId` 作为 key，让同一订单进入同一个 Partition。跨订单可以并行处理，从而兼顾
正确性和吞吐。

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
Kafka 的顺序边界来自 Partition。每个 Partition 是独立追加日志，offset 只在 Partition 内递增。
不同 Partition 可以在不同 Broker 上并行写入和消费，没有全局追加序列。

如果强行要求全局顺序，通常只能使用一个 Partition，这会牺牲吞吐和扩展性。生产系统更常见的是
按订单、支付单或 SKU 保证局部顺序。
```

## 回答评分点

高分答案应该覆盖：

- Partition 是独立日志。
- offset 只在 Partition 内有意义。
- 不同 Partition 并行处理。
- 全局顺序需要牺牲吞吐。
- 业务通常只需要局部顺序。

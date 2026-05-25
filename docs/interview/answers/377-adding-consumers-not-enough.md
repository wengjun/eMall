# 377 增加消费者为什么不一定能解决积压？

[返回按分类学习面试题](../README.md)

## 题目

增加消费者为什么不一定能解决积压？

## 先给面试官的短答案

增加消费者不一定解决积压，因为同一个 Consumer Group 的最大并行度受 Partition 数量限制。
如果消费者数量超过 Partition 数量，多出来的消费者不会分到 Partition。

另外，瓶颈可能在下游、数据库、单分区热点或慢消息，不在消费者实例数量。

## Partition 限制

规则：

- 同组内一个 Partition 同时只给一个 Consumer。
- Consumer 数量大于 Partition 数量时，部分 Consumer 空闲。
- 单个热点 Partition 不能被多个 Consumer 同时消费。

所以并行度首先看 Partition。

## 下游瓶颈

如果下游慢：

- 增加消费者会增加下游并发。
- 下游可能被打垮。
- 错误率上升。
- 重试消息更多。
- 积压反而更严重。

扩容前要确认下游容量。

## 其他原因

其他原因：

- poison message 阻塞。
- 消费逻辑串行锁竞争。
- 数据库连接池不足。
- offset 提交失败。
- Rebalance 频繁。
- 单 key 顺序处理太慢。

这些问题不靠简单加实例解决。

## 在 eMall 项目中怎么讲？

eMall 搜索索引 Topic 如果只有 8 个 Partition，部署 20 个同组消费者也只有最多 8 个消费者工作。

如果真正瓶颈是 OpenSearch bulk 写入慢，就应该优化 bulk、扩容搜索集群或限速消费，而不是继续
增加消费者。

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
增加消费者不一定解决积压，因为 Kafka 同组并行度受 Partition 数量限制，一个 Partition 同时只
能由一个 Consumer 消费。消费者超过 Partition 数量会空闲。

此外瓶颈可能在下游服务、数据库、单分区热点、慢消息或 Rebalance。正确做法是先定位瓶颈，再
决定扩容消费者、增加 Partition、批量化处理或治理下游。
```

## 回答评分点

高分答案应该覆盖：

- 并行度受 Partition 数量限制。
- 多余消费者会空闲。
- 单分区热点无法靠加消费者解决。
- 下游慢时加消费者会放大故障。
- 需要先定位瓶颈。

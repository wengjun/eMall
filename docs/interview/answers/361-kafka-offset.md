# 361 offset 是什么？

[返回按分类学习面试题](../README.md)

## 题目

offset 是什么？

## 先给面试官的短答案

offset 是 Kafka Partition 内每条消息的顺序位置编号。它只在单个 Partition 内有意义，不是全局
递增 ID。

消费者通过提交 offset 记录自己消费到哪里，下次继续从该位置之后消费。

## offset 的作用

作用：

- 标识消息在 Partition 内的位置。
- 支持消费者断点续传。
- 支持重复消费或回溯消费。
- 支持消费进度监控。
- 支持 lag 计算。

offset 是 Kafka 消费状态管理的核心。

## offset 的范围

需要注意：

- offset 属于 Partition。
- 不同 Partition 的 offset 不能比较大小。
- 同一个 Topic 内没有全局 offset。
- 消息顺序也只在 Partition 内保证。

这就是 Kafka 顺序性的边界。

## committed offset

committed offset 表示消费者组已经提交的消费位置。

如果消费者重启，会从 committed offset 之后继续消费。提交过早可能丢消息，提交过晚可能重复消费。

## 在 eMall 项目中怎么讲？

eMall 的订单事件 Topic 有多个 Partition。`order-created` 某条消息在 Partition 3 的 offset 为
1024，只表示它在 Partition 3 中的位置。

`inventory-group` 和 `fulfillment-group` 是不同消费组，它们各自维护自己的 offset。

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
offset 是 Kafka 单个 Partition 内的消息位置编号。消费者组通过提交 offset 记录消费进度，从而
实现重启后的断点续传和消费 lag 监控。

offset 不是全局 ID，不同 Partition 的 offset 没有可比性。提交时机决定消息语义，提交太早可能
丢消息，提交太晚可能重复消费。
```

## 回答评分点

高分答案应该覆盖：

- offset 是 Partition 内位置。
- 不是全局递增 ID。
- Consumer Group 维护提交进度。
- 可用于 lag 计算。
- 提交时机影响丢失和重复。

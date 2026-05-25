# 365 ISR 是什么？

[返回按分类学习面试题](../README.md)

## 题目

ISR 是什么？

## 先给面试官的短答案

ISR 是 In-Sync Replicas，表示与 leader 保持同步的一组副本。Kafka 只会从 ISR 中选择新的 leader，
以降低故障切换后的数据丢失风险。

ISR 不是所有副本，而是当前跟得上 leader 的副本集合。

## Replica 角色

一个 Partition 有：

- leader replica。
- follower replica。
- ISR 集合。

生产和消费通常走 leader，follower 从 leader 拉取数据。

## ISR 的意义

意义：

- 判断哪些副本足够同步。
- 控制 ack=all 的写入确认。
- 支持 leader 故障后的安全选主。
- 反映复制健康状态。

ISR 过小代表副本同步能力下降。

## 影响因素

可能导致副本离开 ISR：

- follower 拉取落后。
- 网络抖动。
- 磁盘 IO 慢。
- Broker 负载高。
- GC 或进程暂停。

ISR 波动需要告警。

## 在 eMall 项目中怎么讲？

eMall 的订单事件 Topic 应配置合理副本数和 `min.insync.replicas`。如果 ISR 数量不足，核心事件写入
应该失败或降级，而不是假装写入成功。

这样可以避免 Broker 故障后丢失订单事件。

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
ISR 是与 leader 保持同步的副本集合。Kafka 会基于 ISR 做 ack=all 的确认和故障后的 leader 选举。
只有在 ISR 中的副本才被认为足够新。

生产中要监控 ISR shrink、under replicated partitions 和复制延迟。核心订单事件要配合
min.insync.replicas，避免副本不足时仍然确认写入。
```

## 回答评分点

高分答案应该覆盖：

- ISR 是同步副本集合。
- 不是所有副本。
- leader 故障从 ISR 选主。
- ack=all 和 ISR 有关。
- 核心 Topic 要监控 ISR。

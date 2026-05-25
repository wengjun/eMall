# 375 consumer lag 如何监控？

[返回按分类学习面试题](../README.md)

## 题目

consumer lag 如何监控？

## 先给面试官的短答案

consumer lag 是消费者组已提交 offset 与 Partition 最新 offset 之间的差距，表示还有多少消息未被
该消费者组处理。监控要按 Topic、Consumer Group、Partition 维度展开。

只看总 lag 不够，还要看增长速度、持续时间、最大分区 lag 和消费耗时。

## 关键指标

指标：

- 总 lag。
- 单 Partition lag。
- lag 增长速度。
- 消费 TPS。
- 单条处理耗时。
- offset 提交延迟。
- Rebalance 次数。
- 消费错误率。

最大分区 lag 很重要，因为它可能暴露热点分区。

## 告警策略

告警要考虑：

- lag 超过绝对阈值。
- lag 持续增长。
- lag 持续时间超过阈值。
- 核心 Topic 使用更严格阈值。
- 非核心 Topic 允许更长延迟。

只用一个全局阈值容易误报或漏报。

## 监控关联

lag 告警后要关联：

- Producer 写入速率。
- Consumer 消费速率。
- Broker 状态。
- 下游服务延迟。
- 数据库延迟。
- 部署和变更时间。

lag 是结果指标，需要结合原因指标排查。

## 在 eMall 项目中怎么讲？

eMall 的 `order-events` 需要按消费组监控 lag。库存组 lag 高会影响库存释放或扣减，搜索组 lag 高
会影响搜索结果新鲜度。

不同消费组业务影响不同，告警等级也不同。

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
consumer lag 是最新 offset 和消费组已提交 offset 的差值。监控要按 Topic、Group 和 Partition
维度看总 lag、最大分区 lag、增长速度、持续时间、消费 TPS、处理耗时和错误率。

告警不能只看绝对值，要结合业务重要性和增长趋势。排查时还要关联 Producer 写入速率、下游延迟、
数据库慢查询和部署变更。
```

## 回答评分点

高分答案应该覆盖：

- lag 是最新 offset 与提交 offset 差值。
- 按 Topic、Group、Partition 监控。
- 看增长速度和持续时间。
- 最大分区 lag 暴露热点。
- 结合业务重要性告警。

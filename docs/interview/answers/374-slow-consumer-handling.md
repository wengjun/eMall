# 374 消费者处理慢怎么办？

[返回按分类学习面试题](../README.md)

## 题目

消费者处理慢怎么办？

## 先给面试官的短答案

消费者处理慢要先定位瓶颈，是消费逻辑慢、下游慢、数据库慢、单分区热点、批量配置不合理，还是
Rebalance 频繁。处理方式包括优化业务逻辑、批量处理、增加并行度、扩容消费者、拆分 Topic、
治理下游和隔离慢消息。

不能盲目加消费者，因为并行度受 Partition 数量和热点分布限制。

## 排查方向

方向：

- Consumer lag 是否持续增加。
- 单条处理耗时。
- 下游接口耗时。
- 数据库慢 SQL。
- 是否有 poison message。
- Partition 是否倾斜。
- Rebalance 是否频繁。

先找到慢在哪里。

## 优化方式

方式：

- 批量拉取和批量写入。
- 减少同步下游调用。
- 使用本地缓存降低查询成本。
- 增加 Consumer 实例。
- 增加 Partition 并重新分布。
- 慢消息进入重试或死信。
- 对热点 key 单独治理。

优化要避免破坏消息顺序和幂等。

## 下游慢

如果是下游慢：

- 对下游限流。
- 熔断和降级。
- 异步化处理。
- 分离核心和非核心消费。
- 建立重试 Topic。

Consumer 不能无限压垮下游。

## 在 eMall 项目中怎么讲？

eMall 搜索索引消费者处理慢时，先看是 OpenSearch 写入慢，还是商品服务回查慢。

可以把单条写入改为批量 bulk，增加消费者实例，并把反序列化失败或非法商品消息送入死信 Topic，
避免阻塞正常消息。

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
消费者处理慢要先定位瓶颈，包括业务处理耗时、下游慢、数据库慢、单分区热点、配置不合理和频繁
Rebalance。处理上可以批量化、减少同步调用、扩容消费者、增加 Partition、治理热点和隔离慢消息。

不能只说加机器，因为同一个消费组的并行度受 Partition 数量限制，而且下游慢时加消费者可能把
下游打垮。
```

## 回答评分点

高分答案应该覆盖：

- 先定位瓶颈。
- 关注 lag、处理耗时和下游耗时。
- 批量化和扩容。
- Partition 限制并行度。
- 慢消息要隔离。

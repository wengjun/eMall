# 331 Redis Cluster 如何分片？

[返回按分类学习面试题](../README.md)

## 题目

Redis Cluster 如何分片？

## 先给面试官的短答案

Redis Cluster 通过 16384 个 hash slot 做数据分片。每个 key 经过 CRC16 计算后对 16384 取模，
得到对应 slot，再由负责该 slot 的主节点处理请求。

Redis Cluster 的核心是 slot 归属，而不是简单按节点数量取模。

## 分片机制

流程：

- 客户端对 key 计算 CRC16。
- 对 16384 取模得到 slot。
- 根据 slot 路由到对应主节点。
- 主节点负责该 slot 的读写。
- 从节点复制主节点数据。
- slot 可以迁移到其他节点。

slot 让扩容迁移以 slot 为单位进行。

## hash tag

hash tag 用于让多个 key 落到同一个 slot。

例如：

```text
cart:{user123}:items
cart:{user123}:meta
```

只有 `{user123}` 参与 slot 计算。这样可以让同一用户购物车相关 key 在同一分片。

## 生产注意点

注意：

- 多 key 操作要求 key 在同一 slot。
- 热 key 仍可能打满单个分片。
- 扩容时要迁移 slot。
- 客户端要支持 MOVED 和 ASK 重定向。
- 不能把 Cluster 当强一致存储。

分片解决容量问题，不自动解决热点问题。

## 在 eMall 项目中怎么讲？

eMall 商品详情缓存可以按商品 ID 分散到不同 slot。购物车可以用 hash tag 让同一用户的多个 key
落在同一 slot，方便局部多 key 操作。

秒杀热点 SKU 不能只靠 Cluster，因为一个热点 key 仍然会落到一个 slot，需要做本地缓存、
热点复制或令牌化削峰。

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
Redis Cluster 使用 16384 个 hash slot 做分片。客户端根据 key 计算 CRC16，再对 16384 取模，
找到 slot 对应的主节点。扩容和迁移以 slot 为单位。

它解决的是容量和吞吐水平扩展问题，但不等于自动解决热 key。生产中要同时考虑 hash tag、
客户端重定向、slot 迁移、主从复制和热点治理。
```

## 回答评分点

高分答案应该覆盖：

- 16384 个 slot。
- CRC16 计算 slot。
- slot 归属到主节点。
- hash tag 的作用。
- 分片不等于热点治理。

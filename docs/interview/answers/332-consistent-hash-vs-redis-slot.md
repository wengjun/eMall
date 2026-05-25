# 332 一致性哈希和 Redis Cluster slot 有什么区别？

[返回按分类学习面试题](../README.md)

## 题目

一致性哈希和 Redis Cluster slot 有什么区别？

## 先给面试官的短答案

一致性哈希是通用的分布式哈希算法，通过哈希环降低节点变更带来的数据迁移量。Redis Cluster
使用固定的 16384 个 slot，key 先映射到 slot，再由 slot 映射到节点。

一致性哈希强调算法思想，Redis slot 强调可管理的分片单元。

## 一致性哈希

特点：

- 节点和 key 都映射到哈希环。
- key 顺时针找到第一个节点。
- 节点变更只影响局部数据。
- 通常使用虚拟节点改善均衡性。

它适合很多分布式缓存和路由场景。

## Redis Cluster slot

特点：

- slot 数固定为 16384。
- key 先映射到 slot。
- slot 再分配给节点。
- 扩容时迁移 slot。
- 客户端可缓存 slot 到节点的映射。

slot 是一个显式的运维管理单元。

## 核心区别

区别：

- 一致性哈希通常是节点直接参与路由。
- Redis Cluster 多了一层固定 slot。
- slot 迁移和观测更明确。
- Redis Cluster 支持 MOVED 和 ASK 重定向。
- 一致性哈希需要额外处理虚拟节点和元数据同步。

二者目标都包括降低扩容迁移成本。

## 在 eMall 项目中怎么讲？

如果 eMall 自研本地缓存路由，可以使用一致性哈希减少节点变化影响。

如果使用 Redis Cluster，就按 slot 机制理解和运维。扩容时关注 slot 迁移进度、热点 slot 和
客户端重定向，而不是只说增加机器。

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
一致性哈希是一种通用路由算法，把 key 和节点映射到哈希环，节点变化时尽量只迁移局部数据。
Redis Cluster 采用固定 16384 个 slot，key 映射到 slot，slot 再映射到节点。

slot 的好处是分片管理更明确，扩容迁移以 slot 为单位，客户端也能缓存 slot 路由。两者都在解决
节点变化时的数据迁移问题，但 Redis Cluster 更工程化。
```

## 回答评分点

高分答案应该覆盖：

- 一致性哈希的哈希环。
- 虚拟节点改善均衡。
- Redis Cluster 固定 slot。
- slot 是可迁移管理单元。
- 能说明二者不是同一个东西。

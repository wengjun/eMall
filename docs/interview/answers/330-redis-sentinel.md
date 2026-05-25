# 330 Redis Sentinel 解决什么问题？

[返回按分类学习面试题](../README.md)

## 题目

Redis Sentinel 解决什么问题？

## 先给面试官的短答案

Redis Sentinel 主要解决 Redis 主从架构下的高可用问题，包括监控主从节点、发现主节点故障、
选举新主节点、通知客户端新的主节点地址。

它不解决数据分片和容量扩展问题，分片扩展通常要使用 Redis Cluster。

## 核心能力

Sentinel 提供：

- 监控 Redis 节点。
- 主观下线判断。
- 客观下线判断。
- Sentinel 之间协商。
- 自动故障转移。
- 从节点提升为新主。
- 通知客户端主节点变化。

多个 Sentinel 共同判断可以降低误判风险。

## 故障切换过程

过程包括：

- Sentinel 发现主节点不可达。
- 多个 Sentinel 达成客观下线判断。
- 选举一个 Sentinel 作为 leader。
- leader 选择合适从节点提升为主。
- 其他从节点改为复制新主。
- 客户端更新主节点连接。

故障切换期间可能出现短暂不可用。

## 边界

Sentinel 不解决：

- 数据自动分片。
- 单实例容量瓶颈。
- 强一致复制。
- 热 key 打满单节点。
- 客户端重试风暴。

Sentinel 是高可用方案，不是水平扩展方案。

## 在 eMall 项目中怎么讲？

eMall 如果使用单主多从 Redis 缓存集群，可以用 Sentinel 做主节点故障转移。

但商品详情、购物车、限流等流量很大时，单主容量可能不够，需要 Redis Cluster 分片。
Sentinel 只能提高主从高可用，不能支撑无限容量。

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
Redis Sentinel 解决的是主从架构高可用。它会监控 Redis 主从节点，在主节点故障时通过多个
Sentinel 达成客观下线判断，再选举 leader 执行故障转移，把一个从节点提升为新主。

但 Sentinel 不提供数据分片，也不解决单节点容量和热 key 问题。高并发电商系统通常需要结合
Redis Cluster、客户端重试控制和降级策略一起使用。
```

## 回答评分点

高分答案应该覆盖：

- Sentinel 负责高可用。
- 说明主观下线和客观下线。
- 说明自动故障转移过程。
- 明确 Sentinel 不负责分片。
- 能和 Redis Cluster 做边界区分。

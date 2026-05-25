# 344 热 key 如何治理？

[返回按分类学习面试题](../README.md)

## 题目

热 key 如何治理？

## 先给面试官的短答案

热 key 治理的目标是避免单个 key、单个分片或单个后端被流量打爆。常见手段包括本地缓存、
热点副本、请求合并、逻辑过期、异步刷新、限流、降级和业务削峰。

Redis Cluster 分片不能自动解决单 key 热点，因为一个 key 仍然只落在一个 slot。

## 治理手段

手段包括：

- 应用本地缓存。
- 多副本热点 key。
- 请求合并。
- 逻辑过期。
- 异步刷新。
- CDN 缓存静态内容。
- 限流和排队。
- 降级返回旧值。

不同业务选择不同组合。

## 热点副本

热点副本是把一个逻辑 key 拆成多个物理 key。

例如：

```text
product:detail:10001:copy:0
product:detail:10001:copy:1
product:detail:10001:copy:2
```

读请求随机访问不同副本，从而把压力分散到多个 slot。

## 注意点

注意：

- 本地缓存会带来更多一致性延迟。
- 副本 key 更新要同步。
- 热点库存不能简单复制扣减。
- 降级要避免错误承诺。
- 限流要保护核心链路。

热点治理要区分读热点和写热点。

## 在 eMall 项目中怎么讲？

商品详情读热点可以用本地缓存和热点副本。秒杀库存写热点不能靠复制库存 key 解决，而要使用
令牌、库存桶、队列削峰和条件扣减。

首页活动配置可以降级返回旧值，但支付结果不能靠旧缓存降级。

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
热 key 治理要先区分读热点和写热点。读热点可以通过本地缓存、热点副本、请求合并、逻辑过期和
异步刷新分摊压力。写热点要通过分桶、队列、令牌和限流削峰。

Redis Cluster 只能分散不同 key，不能把一个 key 自动拆开。生产中还要配合监控自动发现和动态
治理策略。
```

## 回答评分点

高分答案应该覆盖：

- Redis Cluster 不能自动解决单 key 热点。
- 本地缓存和热点副本。
- 请求合并和逻辑过期。
- 区分读热点和写热点。
- 秒杀库存要用削峰和分桶。

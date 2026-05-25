# 356 Redis 内存淘汰策略有哪些？

[返回按分类学习面试题](../README.md)

## 题目

Redis 内存淘汰策略有哪些？

## 先给面试官的短答案

Redis 内存达到 `maxmemory` 后，会根据淘汰策略选择 key 删除。常见策略包括 noeviction、allkeys-lru、
volatile-lru、allkeys-lfu、volatile-lfu、allkeys-random、volatile-random 和 volatile-ttl。

策略选择要看 Redis 是否只做缓存，以及哪些 key 允许被淘汰。

## 策略分类

按范围分：

- allkeys：所有 key 都可能被淘汰。
- volatile：只有设置过期时间的 key 可能被淘汰。
- noeviction：不淘汰，写入报错。

按算法分：

- LRU：淘汰最近最少使用。
- LFU：淘汰访问频率低。
- random：随机淘汰。
- ttl：淘汰剩余 TTL 更短的 key。

## 如何选择

选择建议：

- 纯缓存场景可用 allkeys-lru 或 allkeys-lfu。
- 只有部分 key 可淘汰时用 volatile 策略。
- 不允许丢数据时用 noeviction。
- 热点明显时 LFU 可能更合适。

但更重要的是不要把 Redis 内存长期打满。

## 风险

风险包括：

- 淘汰关键 key 导致业务异常。
- 命中率下降导致数据库压力上升。
- 未设置 TTL 的 key 无法被 volatile 策略淘汰。
- 大 key 让内存倾斜更严重。

淘汰策略不是容量规划的替代品。

## 在 eMall 项目中怎么讲？

eMall 商品详情缓存可以接受淘汰，但分布式锁、限流计数和关键短期状态不应和普通缓存混在同一个
Redis 实例中。

更好的做法是按用途拆分 Redis 集群，缓存集群使用合适淘汰策略，锁和限流集群严格控制内存和 TTL。

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
Redis 内存淘汰策略分为 noeviction、allkeys 系列和 volatile 系列。allkeys 会在所有 key 中淘汰，
volatile 只淘汰设置了过期时间的 key，算法包括 LRU、LFU、random 和 ttl。

生产选择要看数据是否允许丢。普通缓存可以淘汰，锁、限流和短期状态要谨慎。最好按用途拆分 Redis，
并通过容量规划避免长期依赖淘汰策略。
```

## 回答评分点

高分答案应该覆盖：

- noeviction、allkeys、volatile。
- LRU、LFU、random、ttl。
- 纯缓存和状态数据策略不同。
- 关键 key 不应随意淘汰。
- 淘汰不是容量规划替代品。

# 347 Redis 分布式锁如何实现？

[返回按分类学习面试题](../README.md)

## 题目

Redis 分布式锁如何实现？

## 先给面试官的短答案

Redis 分布式锁通常使用 `SET key value NX PX ttl` 实现加锁，value 使用唯一请求标识。释放锁时
用 Lua 脚本先比较 value 是否一致，再删除 key，避免误删别人的锁。

锁必须设置过期时间，并且业务执行时间要小于锁 TTL 或具备续期机制。

## 加锁

加锁命令：

```text
SET lock:order:10001 request-uuid NX PX 3000
```

含义：

- NX 表示 key 不存在才设置。
- PX 表示毫秒级过期时间。
- value 用来标识锁持有者。

只有返回成功才算拿到锁。

## 解锁

解锁要用 Lua：

```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

原因是比较 value 和删除必须原子执行。

## 风险

风险包括：

- 锁 TTL 过短导致业务未完成锁已过期。
- 业务卡住导致锁被长时间持有。
- 误删其他线程锁。
- Redis 主从切换导致锁状态丢失。
- 锁粒度过粗影响吞吐。

分布式锁不能替代数据库约束和幂等。

## 在 eMall 项目中怎么讲？

eMall 可以在防重复提交、低频后台任务和活动配置更新中使用 Redis 锁。

但扣库存不能只依赖 Redis 锁，仍要用数据库条件更新或库存服务原子扣减保证不超卖。Redis 锁用于
降低并发冲突，不作为最终一致性证明。

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
Redis 分布式锁的基础实现是 SET key value NX PX ttl。value 必须唯一，释放时通过 Lua 脚本比较
value 后删除，保证不会误删其他线程的锁。

生产中还要考虑 TTL、续期、锁粒度、异常释放、主从切换和业务幂等。锁只能降低并发冲突，不能
替代唯一约束、状态机和条件更新。
```

## 回答评分点

高分答案应该覆盖：

- 使用 SET NX PX。
- value 必须唯一。
- Lua 比较后删除。
- TTL 和续期风险。
- 锁不能替代业务幂等。

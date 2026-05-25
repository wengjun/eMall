# 349 Lua 脚本为什么能保证原子性？

[返回按分类学习面试题](../README.md)

## 题目

Lua 脚本为什么能保证原子性？

## 先给面试官的短答案

Redis 执行 Lua 脚本时，会把脚本作为一个整体在 Redis 执行线程中运行。脚本执行期间不会插入执行
其他客户端命令，因此脚本内部多个 Redis 操作对其他客户端表现为原子。

它保证的是 Redis 单实例内的脚本执行原子性，不等于跨系统事务原子性。

## 为什么原子

原因：

- Redis 命令执行模型是串行的。
- Lua 脚本作为一个命令执行。
- 脚本执行期间不会被其他命令打断。
- 脚本内读写的结果对外一次性生效。

所以比较锁 value 和删除锁可以放在一个脚本里。

## 示例

释放锁脚本：

```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

如果不用 Lua，`GET` 和 `DEL` 之间可能被其他客户端命令插入。

## 边界

边界包括：

- 长 Lua 会阻塞 Redis。
- 脚本不能做耗时计算。
- Cluster 下多 key 要在同一 slot。
- Redis 故障不能保证跨实例事务。
- Lua 不能替代业务幂等。

Lua 要短小、确定、可控。

## 在 eMall 项目中怎么讲？

eMall 可以用 Lua 实现 Redis 锁释放、限流计数和令牌扣减。

例如秒杀令牌扣减时，Lua 可以检查令牌数量并扣减，避免先读后写之间被并发打断。但最终订单创建
仍要依靠幂等和库存扣减确认。

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
Lua 脚本在 Redis 中会作为一个整体命令执行，Redis 在脚本执行期间不会处理其他客户端命令，所以
脚本内部多个操作具有单实例原子性。

这个能力适合做比较后删除锁、限流计数和令牌扣减。但 Lua 不能太长，也不能跨系统保证事务一致。
在 Redis Cluster 中，多 key 脚本还要保证 key 在同一个 slot。
```

## 回答评分点

高分答案应该覆盖：

- Lua 作为一个整体命令执行。
- 执行期间不会被其他命令打断。
- 适合比较后删除锁。
- 长脚本会阻塞 Redis。
- 原子性边界是 Redis 单实例。

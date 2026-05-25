# 348 `SET NX PX` 有什么注意点？

[返回按分类学习面试题](../README.md)

## 题目

`SET NX PX` 有什么注意点？

## 先给面试官的短答案

`SET NX PX` 要注意加锁必须原子、value 必须唯一、TTL 必须合理、释放锁必须比较 value 后删除，
并且业务要能处理锁过期、续期失败和 Redis 故障。

只会写命令不够，关键是理解它在异常场景下的边界。

## 关键点

关键点：

- 使用一条 `SET` 命令完成加锁和过期设置。
- 不要先 `SETNX` 再单独 `EXPIRE`。
- value 使用 UUID 或请求 ID。
- TTL 要覆盖正常业务耗时。
- 解锁必须用 Lua 原子比较和删除。
- 加锁失败要快速返回、排队或重试退避。

加锁和设置过期必须是原子的。

## TTL 选择

TTL 太短：

- 业务未完成锁已过期。
- 其他线程拿到锁并发执行。
- 旧线程结束后可能误删新锁。

TTL 太长：

- 故障后等待时间长。
- 降低系统恢复速度。
- 容易造成队列堆积。

需要结合 P99 耗时和超时控制选择。

## 失败处理

失败处理包括：

- 获取锁失败不要无限自旋。
- 重试要有退避和上限。
- 业务执行要幂等。
- 解锁失败要记录告警。
- Redis 不可用时要降级或拒绝。

锁失败本身也是系统状态。

## 在 eMall 项目中怎么讲？

eMall 防重复提交可以使用 `SET NX PX`。例如同一个用户同一个幂等号只能同时处理一次。

如果获取锁失败，可以返回请求处理中。真正的订单唯一性仍由订单幂等表或唯一索引保证，避免 Redis
异常时产生重复订单。

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
SET NX PX 的重点是原子加锁和自动过期。value 要唯一，释放锁时要通过 Lua 比较 value 后删除。
TTL 要覆盖业务 P99 耗时，但不能过长。

生产中不能无限重试，要有退避、超时和降级。更重要的是，Redis 锁只是并发控制手段，最终正确性
要靠幂等、唯一约束和状态机保证。
```

## 回答评分点

高分答案应该覆盖：

- 一条 SET 命令原子加锁。
- 不要 SETNX 后再 EXPIRE。
- value 唯一。
- Lua 解锁。
- TTL、重试和幂等边界。

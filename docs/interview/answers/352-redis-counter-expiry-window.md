# 352 Redis 计数器如何避免过期窗口问题？

[返回按分类学习面试题](../README.md)

## 题目

Redis 计数器如何避免过期窗口问题？

## 先给面试官的短答案

Redis 计数器常见过期窗口问题是计数自增和设置过期不是原子操作，或者固定窗口边界允许流量突刺。
解决方式包括 Lua 原子自增并设置 TTL、滑动窗口、令牌桶和多窗口组合。

要先区分是 TTL 丢失问题，还是固定窗口边界突刺问题。

## TTL 丢失问题

错误做法：

```text
INCR key
EXPIRE key 60
```

如果 `INCR` 成功后应用崩溃，`EXPIRE` 没执行，key 可能永不过期。

正确做法是 Lua 中判断第一次自增时设置 TTL，保证原子性。

## 固定窗口边界突刺

固定窗口可能出现：

- 00:59 通过 100 次。
- 01:00 又通过 100 次。
- 两秒内实际通过 200 次。

这就是窗口边界突刺。

## 解决方式

方式：

- Lua 原子自增和设置 TTL。
- 滑动窗口按真实时间统计。
- 令牌桶控制平均速率和突发。
- 多窗口组合限制短期和长期流量。
- 本地限流防 Redis 故障。

不同问题要用不同方案。

## 在 eMall 项目中怎么讲？

eMall 登录接口可以用固定窗口加 Lua，防止 TTL 丢失。

下单和领券接口对突刺更敏感，可以用滑动窗口或令牌桶，避免用户在窗口边界集中提交请求。

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
Redis 计数器的过期窗口问题有两类。一类是 INCR 和 EXPIRE 分开执行导致 TTL 丢失，需要用 Lua
把自增和设置过期做成原子操作。另一类是固定窗口边界突刺，需要滑动窗口、令牌桶或多窗口组合。

生产中要根据接口风险选择方案。登录可以简单计数，下单和领券要更平滑，并且要有本地兜底。
```

## 回答评分点

高分答案应该覆盖：

- 区分 TTL 丢失和边界突刺。
- INCR 加 EXPIRE 非原子有风险。
- Lua 原子处理。
- 滑动窗口或令牌桶平滑限流。
- 高风险接口要更严格。

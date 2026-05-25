# 366 ack=0、ack=1、ack=all 有什么区别？

[返回按分类学习面试题](../README.md)

## 题目

ack=0、ack=1、ack=all 有什么区别？

## 先给面试官的短答案

`ack=0` 表示 Producer 不等待 Broker 确认，吞吐高但最容易丢消息。`ack=1` 表示 leader 写入成功
就确认，leader 宕机时仍可能丢。`ack=all` 表示 ISR 中足够副本写入后才确认，可靠性最高但延迟更高。

核心交易事件通常选择 `ack=all` 配合 `min.insync.replicas`。

## ack=0

特点：

- 不等待服务端确认。
- 延迟最低。
- 吞吐高。
- 可能丢消息且生产者不知道。

适合低价值日志，不适合核心业务。

## ack=1

特点：

- leader 写入后确认。
- 性能和可靠性折中。
- follower 还没复制时 leader 宕机可能丢。

适合部分可容忍少量丢失的场景。

## ack=all

特点：

- 等待 ISR 满足确认条件。
- 可靠性最高。
- 延迟更高。
- 需要配置 `min.insync.replicas`。

它是核心事件的常用选择。

## 在 eMall 项目中怎么讲？

eMall 的 `order-created`、`payment-succeeded`、`refund-created` 这类核心事件应使用 `ack=all`。

用户行为日志、曝光日志可以根据成本和重要性选择较低可靠性配置，但要明确丢失影响。

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
ack=0 不等确认，吞吐高但可能静默丢失；ack=1 只等 leader 写入，leader 宕机且 follower 未复制时
可能丢失；ack=all 等 ISR 中足够副本确认，可靠性最高但延迟更高。

核心电商事件应使用 ack=all、合理副本数、min.insync.replicas 和生产者重试，低价值日志可以按
成本选择更低级别。
```

## 回答评分点

高分答案应该覆盖：

- ack=0 不等确认。
- ack=1 leader 确认。
- ack=all ISR 确认。
- 可靠性和延迟权衡。
- 核心事件选 ack=all。

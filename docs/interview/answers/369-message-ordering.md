# 369 消息顺序如何保证？

[返回按分类学习面试题](../README.md)

## 题目

消息顺序如何保证？

## 先给面试官的短答案

Kafka 只能天然保证同一个 Partition 内的消息顺序。要保证同一业务对象有序，需要用稳定 key 把
该对象的消息发送到同一个 Partition，并在消费端单线程或按 key 串行处理。

不要轻易追求全局顺序，全局顺序会严重限制吞吐。

## Producer 侧

Producer 侧要做：

- 使用稳定 message key。
- 同一订单使用同一个 `orderId`。
- 同一用户使用同一个 `userId`。
- 控制重试和幂等配置。
- 避免乱序发送。

同一 key 进入同一 Partition 是基础。

## Broker 侧

Broker 侧：

- 单个 Partition 内日志有序追加。
- 不同 Partition 之间没有顺序保证。
- Partition 数越多，全局顺序越不可能。

顺序性和并行度天然冲突。

## Consumer 侧

Consumer 侧要注意：

- 同一 Partition 不能并发乱序处理。
- 批量处理失败要小心跳过。
- 异步线程池可能打乱顺序。
- 需要按 key 串行执行。

发送有序不代表处理结果一定有序。

## 在 eMall 项目中怎么讲？

订单状态事件应使用 `orderId` 作为 key，保证同一订单的创建、支付、取消和发货事件在一个
Partition 内。

消费端对同一订单按顺序处理，并用状态机拒绝非法状态跳转。即使消息乱序到达，也不能把已取消
订单错误改成已支付。

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
Kafka 的顺序保证边界是单个 Partition。要保证业务对象顺序，就用稳定 key 把同一对象的消息发到
同一 Partition，并确保消费端不并发乱序处理。

全局顺序通常不现实，因为它会把 Topic 限制成一个 Partition，吞吐很低。电商中更常见的是保证
同一订单、同一支付单或同一 SKU 维度的局部顺序。
```

## 回答评分点

高分答案应该覆盖：

- Kafka 只保证 Partition 内顺序。
- 稳定 key 路由到同一 Partition。
- 消费端也不能乱序处理。
- 全局顺序代价高。
- 状态机可以兜底乱序。

# 364 Kafka 的 exactly-once 为什么不等于业务 exactly-once？

[返回按分类学习面试题](../README.md)

## 题目

Kafka 的 exactly-once 为什么不等于业务 exactly-once？

## 先给面试官的短答案

Kafka 的 exactly-once 主要保证 Kafka 内部生产、消费、写回 Kafka 的原子性和幂等性。业务
exactly-once 涉及数据库、缓存、第三方支付、库存服务和外部接口，边界远大于 Kafka。

所以 Kafka exactly-once 不能替代业务幂等、唯一约束和状态机。

## Kafka 能保证什么

Kafka 事务可以保证：

- 消费输入 Topic。
- 处理后写输出 Topic。
- 提交 offset。
- 这些动作在 Kafka 内部形成事务边界。

它适合 Kafka 到 Kafka 的流处理。

## Kafka 不能自动保证什么

不能自动保证：

- 数据库写入不重复。
- 第三方支付不重复扣款。
- Redis 状态不重复更新。
- HTTP 下游接口不重复调用。
- 库存不会重复扣减。

这些在 Kafka 事务边界之外。

## 业务 exactly-once

业务上要靠：

- 幂等号。
- 唯一约束。
- 状态机条件更新。
- 去重表。
- Outbox。
- 事务消息或补偿。
- 对账。

这才是端到端正确性的基础。

## 在 eMall 项目中怎么讲？

eMall 支付成功事件即使 Kafka 保证消息写入不重复，也不能证明订单不会重复更新或退款不会重复发起。

订单服务必须使用支付单号做幂等，状态从 `PENDING_PAYMENT` 条件更新到 `PAID`，重复消息只返回
已处理结果。

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
Kafka exactly-once 的边界主要在 Kafka 内部，尤其是读一个 Topic、处理后写另一个 Topic，并提交
offset 的场景。它不自动覆盖数据库、Redis、HTTP 下游和第三方支付。

业务 exactly-once 要靠幂等号、唯一约束、状态机、Outbox、补偿和对账来实现。面试中不能把中间件
语义直接等同于端到端业务语义。
```

## 回答评分点

高分答案应该覆盖：

- Kafka exactly-once 有边界。
- 主要适合 Kafka 内部流处理。
- 外部数据库和接口不自动保证。
- 业务要靠幂等和唯一约束。
- 能举支付或库存例子。

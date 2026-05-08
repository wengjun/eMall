# 364 Kafka 的 exactly-once 为什么不等于业务 exactly-once？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

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

## 深度完善：面向 L6 的回答框架

围绕「Kafka 的 exactly-once 为什么不等于业务 exactly-once？」，高分答案不能停在概念定义，而要把「Topic、分区、offset、重试、DLQ、顺序、幂等、Schema 演进和重放」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「消息队列和事件平台」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `event-platform、order outbox、inventory consumer、payment callback 和补偿平台` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：consumer lag、重试次数、DLQ 数、重复消费数、单分区热点、消息端到端延迟。
- 验证证据：消费者幂等记录、DLQ 回放日志、Schema 兼容测试、重放审批和积压处理记录。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：Kafka 的 exactly-once 为什么不等于业务 exactly-once？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

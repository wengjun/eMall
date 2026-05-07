# 371 如何设计订单事件 topic？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

如何设计订单事件 topic？

## 先给面试官的短答案

订单事件 Topic 要围绕事件语义、分区键、消息 schema、可靠性、顺序性、订阅方和治理能力设计。
核心原则是同一订单事件按 `orderId` 路由到同一 Partition，保证单订单状态流转有序。

不要只设计一个随意的消息队列，要把它当成跨服务契约。

## Topic 划分

常见划分方式：

- 按领域事件划分，例如 `order-created`、`order-paid`。
- 按订单事件总线划分，例如 `order-events`。
- 按环境和版本区分。
- 按业务重要性区分核心事件和日志事件。

Topic 过细会增加治理成本，过粗会增加消费过滤和 schema 复杂度。

## 分区键

订单事件通常选 `orderId` 作为 key。

好处：

- 同一订单进入同一 Partition。
- 单订单状态事件有序。
- 消费端更容易做状态机处理。

如果按 `userId`，可以保证用户维度顺序，但订单维度可能不够清晰。

## 可靠性配置

核心配置：

- `acks=all`。
- 开启 Producer 幂等。
- 合理副本数。
- 设置 `min.insync.replicas`。
- 发送失败要重试和告警。
- 使用 Outbox 避免本地事务和消息发送不一致。

订单事件不能静默丢失。

## 在 eMall 项目中怎么讲？

eMall 可以设计 `order-events-v1` Topic，key 使用 `orderId`，事件类型字段区分 created、paid、
cancelled、shipped 和 refunded。

订单服务本地事务写入订单表和 Outbox 表，Relay 再投递 Kafka。消费者通过事件 ID 做幂等。

## 深度增强：缓存和消息治理图

![数据库、缓存和消息一致性链路](../../assets/data-cache-mq.svg)

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
订单事件 Topic 要设计事件语义、分区键、schema、可靠性和治理。通常用 orderId 作为 key，让同一
订单的状态事件进入同一个 Partition，保证单订单有序。

核心订单事件要使用 acks=all、Producer 幂等、副本和 min.insync.replicas，并通过 Outbox 保证
数据库事务和事件发布最终一致。消费者还要做幂等和状态机校验。
```

## 回答评分点

高分答案应该覆盖：

- Topic 是跨服务契约。
- `orderId` 作为分区键。
- 单订单顺序。
- 核心事件可靠性配置。
- Outbox 和消费者幂等。

## 深度完善：面向 L6 的回答框架

围绕「如何设计订单事件 topic？」，高分答案不能停在概念定义，而要把「Topic、分区、offset、重试、DLQ、顺序、幂等、Schema 演进和重放」讲成一条可验证的工程链路。
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

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：如何设计订单事件 topic？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

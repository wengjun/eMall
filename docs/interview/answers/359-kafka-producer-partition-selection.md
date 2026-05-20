# 359 Producer 如何选择 partition？

[返回按分类学习面试题](../README.md)

## 题目

Producer 如何选择 partition？

## 先给面试官的短答案

Kafka Producer 选择 partition 通常有几种方式：消息指定 partition、根据 key 哈希选择 partition、
没有 key 时使用默认分区策略在可用 partition 间分配。

如果要求同一业务对象的消息有序，就必须使用稳定 key 让它们进入同一个 partition。

## 常见方式

方式：

- 直接指定 partition。
- 根据 message key 哈希。
- 没有 key 时由默认策略分配。
- 自定义 partitioner。

大多数业务使用 key 哈希。

## key 的选择

常见 key：

- `orderId`。
- `userId`。
- `skuId`。
- `merchantId`。

选择 key 要看你要保证什么维度的顺序，以及是否会产生热点。

## 顺序和热点

同一个 key 进入同一个 partition，可以保证该 key 内消息顺序。

但如果某个 key 特别热，例如秒杀 SKU，可能导致单个 partition 压力过大。顺序和并发之间需要权衡。

## 在 eMall 项目中怎么讲？

订单状态事件可以用 `orderId` 作为 key，保证同一订单的创建、支付、取消、发货事件按顺序处理。

商品库存事件如果用 `skuId` 作为 key，可以保证单 SKU 顺序，但热门 SKU 会形成热点，需要分桶或
业务层削峰。

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
Producer 可以显式指定 partition，也可以根据 key 哈希选择 partition。没有 key 时由默认策略
分散到可用 partition。

业务上最重要的是 key 选择。同一 key 的消息会进入同一 partition，从而保证该 key 内顺序。但 key
过热会造成 partition 热点，所以要在顺序性和并发度之间取舍。
```

## 回答评分点

高分答案应该覆盖：

- 指定 partition。
- 按 key 哈希。
- 无 key 默认分配。
- 同 key 保证同 partition 顺序。
- key 选择可能带来热点。

## 深度完善：面向 L6 的回答框架

围绕「Producer 如何选择 partition？」，高分答案不能停在概念定义，而要把「Topic、分区、offset、重试、DLQ、顺序、幂等、Schema 演进和重放」讲成一条可验证的工程链路。
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
本题复习重点：Producer 如何选择 partition？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

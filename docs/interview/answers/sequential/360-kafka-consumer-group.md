# 360 Consumer Group 如何工作？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

Consumer Group 如何工作？

## 先给面试官的短答案

Consumer Group 是 Kafka 的消费组机制。同一个消费组内，Topic 的每个 Partition 同一时刻只会被
组内一个 Consumer 消费。不同消费组之间互不影响，可以各自完整消费同一 Topic。

它实现了组内负载均衡和组间广播。

## 工作方式

方式：

- Consumer 加入同一个 group。
- Kafka 将 Partition 分配给组内 Consumer。
- 一个 Partition 同时只分配给组内一个 Consumer。
- Consumer 拉取消息并提交 offset。
- Consumer 增减时触发 rebalance。

Partition 数量决定同组最大并行度上限。

## 组内和组间

组内：

- 多个 Consumer 分摊同一 Topic 的 Partition。
- 用于提升消费吞吐。

组间：

- 不同 group 各自消费完整消息。
- 适合订单、履约、搜索索引等多个系统订阅同一事件。

## Rebalance

Rebalance 发生在：

- Consumer 加入。
- Consumer 离开。
- Partition 增加。
- 心跳超时。

Rebalance 期间可能短暂停止消费，所以要控制消费耗时和心跳配置。

## 在 eMall 项目中怎么讲？

eMall 的 `order-created` Topic 可以被多个消费组订阅。

`inventory-group` 扣减库存，`fulfillment-group` 创建履约任务，`search-group` 更新搜索索引。
每个组都能看到完整订单创建事件，但组内多个实例会分摊 Partition。

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
Consumer Group 是 Kafka 的并行消费机制。同一个 group 内，一个 Partition 同时只会被一个
Consumer 消费，从而保证 Partition 内顺序并实现负载均衡。不同 group 之间互不影响，各自消费
完整 Topic。

生产中要关注 partition 数量、consumer 数量、offset 提交和 rebalance。并行度上限由 Partition
数量决定，而 rebalance 会带来短暂停顿。
```

## 回答评分点

高分答案应该覆盖：

- 同组内 Partition 只给一个 Consumer。
- 组内负载均衡。
- 组间广播。
- Partition 数量限制并行度。
- Rebalance 会影响消费。

## 深度完善：面向 L6 的回答框架

围绕「Consumer Group 如何工作？」，高分答案不能停在概念定义，而要把「Topic、分区、offset、重试、DLQ、顺序、幂等、Schema 演进和重放」讲成一条可验证的工程链路。
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

本题复习重点：Consumer Group 如何工作？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

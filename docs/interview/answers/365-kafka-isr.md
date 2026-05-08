# 365 ISR 是什么？

[返回按分类学习面试题](../README.md)

## 题目

ISR 是什么？

## 先给面试官的短答案

ISR 是 In-Sync Replicas，表示与 leader 保持同步的一组副本。Kafka 只会从 ISR 中选择新的 leader，
以降低故障切换后的数据丢失风险。

ISR 不是所有副本，而是当前跟得上 leader 的副本集合。

## Replica 角色

一个 Partition 有：

- leader replica。
- follower replica。
- ISR 集合。

生产和消费通常走 leader，follower 从 leader 拉取数据。

## ISR 的意义

意义：

- 判断哪些副本足够同步。
- 控制 ack=all 的写入确认。
- 支持 leader 故障后的安全选主。
- 反映复制健康状态。

ISR 过小代表副本同步能力下降。

## 影响因素

可能导致副本离开 ISR：

- follower 拉取落后。
- 网络抖动。
- 磁盘 IO 慢。
- Broker 负载高。
- GC 或进程暂停。

ISR 波动需要告警。

## 在 eMall 项目中怎么讲？

eMall 的订单事件 Topic 应配置合理副本数和 `min.insync.replicas`。如果 ISR 数量不足，核心事件写入
应该失败或降级，而不是假装写入成功。

这样可以避免 Broker 故障后丢失订单事件。

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
ISR 是与 leader 保持同步的副本集合。Kafka 会基于 ISR 做 ack=all 的确认和故障后的 leader 选举。
只有在 ISR 中的副本才被认为足够新。

生产中要监控 ISR shrink、under replicated partitions 和复制延迟。核心订单事件要配合
min.insync.replicas，避免副本不足时仍然确认写入。
```

## 回答评分点

高分答案应该覆盖：

- ISR 是同步副本集合。
- 不是所有副本。
- leader 故障从 ISR 选主。
- ack=all 和 ISR 有关。
- 核心 Topic 要监控 ISR。

## 深度完善：面向 L6 的回答框架

围绕「ISR 是什么？」，高分答案不能停在概念定义，而要把「Topic、分区、offset、重试、DLQ、顺序、幂等、Schema 演进和重放」讲成一条可验证的工程链路。
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

本题复习重点：ISR 是什么？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

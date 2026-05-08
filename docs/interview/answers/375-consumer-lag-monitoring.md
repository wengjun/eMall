# 375 consumer lag 如何监控？

[返回按分类学习面试题](../README.md)

## 题目

consumer lag 如何监控？

## 先给面试官的短答案

consumer lag 是消费者组已提交 offset 与 Partition 最新 offset 之间的差距，表示还有多少消息未被
该消费者组处理。监控要按 Topic、Consumer Group、Partition 维度展开。

只看总 lag 不够，还要看增长速度、持续时间、最大分区 lag 和消费耗时。

## 关键指标

指标：

- 总 lag。
- 单 Partition lag。
- lag 增长速度。
- 消费 TPS。
- 单条处理耗时。
- offset 提交延迟。
- Rebalance 次数。
- 消费错误率。

最大分区 lag 很重要，因为它可能暴露热点分区。

## 告警策略

告警要考虑：

- lag 超过绝对阈值。
- lag 持续增长。
- lag 持续时间超过阈值。
- 核心 Topic 使用更严格阈值。
- 非核心 Topic 允许更长延迟。

只用一个全局阈值容易误报或漏报。

## 监控关联

lag 告警后要关联：

- Producer 写入速率。
- Consumer 消费速率。
- Broker 状态。
- 下游服务延迟。
- 数据库延迟。
- 部署和变更时间。

lag 是结果指标，需要结合原因指标排查。

## 在 eMall 项目中怎么讲？

eMall 的 `order-events` 需要按消费组监控 lag。库存组 lag 高会影响库存释放或扣减，搜索组 lag 高
会影响搜索结果新鲜度。

不同消费组业务影响不同，告警等级也不同。

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
consumer lag 是最新 offset 和消费组已提交 offset 的差值。监控要按 Topic、Group 和 Partition
维度看总 lag、最大分区 lag、增长速度、持续时间、消费 TPS、处理耗时和错误率。

告警不能只看绝对值，要结合业务重要性和增长趋势。排查时还要关联 Producer 写入速率、下游延迟、
数据库慢查询和部署变更。
```

## 回答评分点

高分答案应该覆盖：

- lag 是最新 offset 与提交 offset 差值。
- 按 Topic、Group、Partition 监控。
- 看增长速度和持续时间。
- 最大分区 lag 暴露热点。
- 结合业务重要性告警。

## 深度完善：面向 L6 的回答框架

围绕「consumer lag 如何监控？」，高分答案不能停在概念定义，而要把「Topic、分区、offset、重试、DLQ、顺序、幂等、Schema 演进和重放」讲成一条可验证的工程链路。
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

本题复习重点：consumer lag 如何监控？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

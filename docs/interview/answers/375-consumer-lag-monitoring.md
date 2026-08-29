# 375 consumer lag 如何监控？

[返回按分类学习面试题](../README.md)

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

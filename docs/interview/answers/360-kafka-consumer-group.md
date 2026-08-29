# 360 Consumer Group 如何工作？

[返回按分类学习面试题](../README.md)

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

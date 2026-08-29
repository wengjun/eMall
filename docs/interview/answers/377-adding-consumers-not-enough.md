# 377 增加消费者为什么不一定能解决积压？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

增加消费者不一定解决积压，因为同一个 Consumer Group 的最大并行度受 Partition 数量限制。
如果消费者数量超过 Partition 数量，多出来的消费者不会分到 Partition。

另外，瓶颈可能在下游、数据库、单分区热点或慢消息，不在消费者实例数量。

## Partition 限制

规则：

- 同组内一个 Partition 同时只给一个 Consumer。
- Consumer 数量大于 Partition 数量时，部分 Consumer 空闲。
- 单个热点 Partition 不能被多个 Consumer 同时消费。

所以并行度首先看 Partition。

## 下游瓶颈

如果下游慢：

- 增加消费者会增加下游并发。
- 下游可能被打垮。
- 错误率上升。
- 重试消息更多。
- 积压反而更严重。

扩容前要确认下游容量。

## 其他原因

其他原因：

- poison message 阻塞。
- 消费逻辑串行锁竞争。
- 数据库连接池不足。
- offset 提交失败。
- Rebalance 频繁。
- 单 key 顺序处理太慢。

这些问题不靠简单加实例解决。

## 在 eMall 项目中怎么讲？

eMall 搜索索引 Topic 如果只有 8 个 Partition，部署 20 个同组消费者也只有最多 8 个消费者工作。

如果真正瓶颈是 OpenSearch bulk 写入慢，就应该优化 bulk、扩容搜索集群或限速消费，而不是继续
增加消费者。

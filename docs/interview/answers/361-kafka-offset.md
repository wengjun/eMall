# 361 offset 是什么？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

offset 是 Kafka Partition 内每条消息的顺序位置编号。它只在单个 Partition 内有意义，不是全局
递增 ID。

消费者通过提交 offset 记录自己消费到哪里，下次继续从该位置之后消费。

## offset 的作用

作用：

- 标识消息在 Partition 内的位置。
- 支持消费者断点续传。
- 支持重复消费或回溯消费。
- 支持消费进度监控。
- 支持 lag 计算。

offset 是 Kafka 消费状态管理的核心。

## offset 的范围

需要注意：

- offset 属于 Partition。
- 不同 Partition 的 offset 不能比较大小。
- 同一个 Topic 内没有全局 offset。
- 消息顺序也只在 Partition 内保证。

这就是 Kafka 顺序性的边界。

## committed offset

committed offset 表示消费者组已经提交的消费位置。

如果消费者重启，会从 committed offset 之后继续消费。提交过早可能丢消息，提交过晚可能重复消费。

## 电商系统实践

大型电商系统的订单事件 Topic 有多个 Partition。`order-created` 某条消息在 Partition 3 的 offset 为
1024，只表示它在 Partition 3 中的位置。

`inventory-group` 和 `fulfillment-group` 是不同消费组，它们各自维护自己的 offset。

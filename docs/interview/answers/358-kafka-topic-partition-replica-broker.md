# 358 Kafka 的 Topic、Partition、Replica、Broker 是什么？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Topic 是消息的逻辑分类，Partition 是 Topic 的物理分片，Replica 是 Partition 的副本，Broker 是
Kafka 集群中的服务器节点。

Kafka 通过 Partition 实现并行读写和水平扩展，通过 Replica 实现高可用。

## Topic

Topic 表示一类消息。

例如：

- `order-created`。
- `payment-succeeded`。
- `inventory-deducted`。
- `product-changed`。

生产者向 Topic 写消息，消费者订阅 Topic。

## Partition

Partition 是 Topic 的分片。

特点：

- 一个 Topic 可以有多个 Partition。
- 每个 Partition 内消息有顺序。
- 不同 Partition 之间没有全局顺序。
- Partition 是并行消费的基础。

吞吐能力通常和 Partition 数量相关。

## Replica 和 Broker

Replica 是 Partition 的副本。

Broker 是 Kafka 服务器节点。一个 Partition 有一个 leader replica 和多个 follower replica。
生产和消费通常访问 leader，follower 从 leader 复制数据。

## 在 eMall 项目中怎么讲？

eMall 可以把订单创建事件写入 `order-created` Topic。该 Topic 按 `orderId` 或 `userId` 分配到
不同 Partition，提升并发处理能力。

每个 Partition 配置多个 Replica，某个 Broker 宕机后，其他副本可以选出新 leader 保持可用。

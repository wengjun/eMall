# 365 ISR 是什么？

[返回按分类学习面试题](../README.md)

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

## 电商系统实践

大型电商系统的订单事件 Topic 应配置合理副本数和 `min.insync.replicas`。如果 ISR 数量不足，核心事件写入
应该失败或降级，而不是假装写入成功。

这样可以避免 Broker 故障后丢失订单事件。

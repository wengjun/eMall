# 444 Kafka lag 增长如何排查？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Kafka lag 增长要先判断是生产速率突增还是消费速率下降，再按 Topic、Consumer Group、Partition
维度看 lag 分布。然后检查消费者耗时、错误率、下游依赖、Rebalance、单分区热点和 Broker 状态。

总 lag 增长只是现象，原因通常在消费端或下游。

## 先看趋势

先看：

- Producer 写入 TPS。
- Consumer 消费 TPS。
- lag 增长速度。
- 最大 Partition lag。
- lag 持续时间。

如果写入突增，可能是流量问题；如果消费下降，重点看消费者。

## 消费端排查

检查：

- 消费者实例是否减少。
- 单条处理耗时是否升高。
- 下游接口是否变慢。
- 数据库是否慢。
- 是否有毒消息反复失败。
- offset 提交是否异常。

消费端慢是最常见原因。

## Kafka 侧排查

检查：

- Broker CPU。
- 磁盘 IO。
- 网络带宽。
- under replicated partitions。
- Rebalance 次数。
- Partition 倾斜。

Broker 异常也会影响消费。

## 止血和恢复

- 分区数足够时扩容消费者；热点分区应修正分区键，增加超过分区数的实例没有收益。
- 隔离反复失败的毒消息，对非核心生产者限速，为核心事件保留 Broker 和消费者容量。
- 积压重放必须限速且保持幂等，恢复后检查过期消息、DLQ 和业务对账差异。

## 电商系统实践

大型电商系统的 `order-events` 的搜索消费组 lag 增长时，先看是否只有搜索组积压。如果库存组正常，说明订单
事件生产正常，问题可能在搜索消费者或 OpenSearch。

如果只有某个 Partition lag 高，重点排查分区键热点。

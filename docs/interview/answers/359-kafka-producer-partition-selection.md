# 359 Producer 如何选择 partition？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Kafka Producer 选择 partition 通常有几种方式：消息指定 partition、根据 key 哈希选择 partition、
没有 key 时使用默认分区策略在可用 partition 间分配。

如果要求同一业务对象的消息有序，就必须使用稳定 key 让它们进入同一个 partition。

## 常见方式

方式：

- 直接指定 partition。
- 根据 message key 哈希。
- 没有 key 时由默认策略分配。
- 自定义 partitioner。

大多数业务使用 key 哈希。

## key 的选择

常见 key：

- `orderId`。
- `userId`。
- `skuId`。
- `merchantId`。

选择 key 要看你要保证什么维度的顺序，以及是否会产生热点。

## 顺序和热点

同一个 key 进入同一个 partition，可以保证该 key 内消息顺序。

但如果某个 key 特别热，例如秒杀 SKU，可能导致单个 partition 压力过大。顺序和并发之间需要权衡。

## 在 eMall 项目中怎么讲？

订单状态事件可以用 `orderId` 作为 key，保证同一订单的创建、支付、取消、发货事件按顺序处理。

商品库存事件如果用 `skuId` 作为 key，可以保证单 SKU 顺序，但热门 SKU 会形成热点，需要分桶或
业务层削峰。

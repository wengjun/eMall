# 378 单分区热点如何处理？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

单分区热点通常由分区键倾斜导致，例如所有秒杀 SKU 消息都使用同一个 `skuId` 作为 key。处理方式
包括调整分区键、热点 key 加桶、拆分 Topic、业务削峰、异步聚合和专门的热点处理链路。

核心是分散热点，同时明确顺序性会被如何影响。

## 发现方式

表现：

- 某个 Partition lag 远高于其他 Partition。
- 某个 Consumer 负载很高。
- Topic 总 lag 不大但最大分区 lag 很大。
- 某个业务 key 请求量异常。

要看 Partition 维度，而不是只看总量。

## 治理方式

方式：

- 调整 message key。
- 对热点 key 加 bucket。
- 增加 Partition 并重分布。
- 单独拆热点 Topic。
- 秒杀请求先削峰。
- 消费端按桶并行处理。

如果必须保证单 key 严格顺序，分桶会破坏这个顺序，需要业务兜底。

## 分桶示例

原 key：

```text
skuId
```

分桶 key：

```text
skuId + ":" + bucketNo
```

这样可以把同一热点 SKU 分散到多个 Partition。

## 电商系统实践

大型电商系统秒杀库存事件如果都按 `skuId` 分区，爆品会压垮单个 Partition。

可以按库存桶或令牌桶编号分区，例如 `skuId:bucketNo`。最终库存一致性由库存桶汇总、条件扣减和
对账补偿保证，而不是依赖单 Partition 串行。

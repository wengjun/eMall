# 699 OpenSearch Doc Values、Fielddata 和 Routing 如何影响查询？

[返回按分类学习面试题](../README.md)

## 倒排索引和列式数据解决不同问题

倒排索引从 term 找文档，适合全文检索；排序、聚合和脚本需要从文档快速取某字段值。Doc Values 是索引时构建的磁盘列式结构，通常用于 `keyword`、数值、日期等字段的排序和聚合，并可借助操作系统页缓存。

`text` 字段面向分词检索，默认不提供适合聚合的 Doc Values。打开 fielddata 会在查询时从倒排索引构建堆内结构，
可能瞬间消耗大量 JVM heap。因此商品名称聚合应使用 `name.keyword` 一类 multi-field，
而不是在 `name` 上开启 fielddata。

```json
{
  "name": {
    "type": "text",
    "fields": {
      "keyword": { "type": "keyword", "ignore_above": 256 }
    }
  }
}
```

## Routing 决定请求触达哪些 shard

默认路由根据文档 `_id` 计算 primary shard。指定自定义 routing 后，同一 routing key 的文档落到确定 shard；
查询若携带相同 routing，只需访问相关 shard，而不是 scatter-gather 所有 shard。

自定义 routing 是读写协议的一部分。创建文档时使用了 routing，后续 GET、更新、删除和查询也必须携带相同值，否则可能“查不到”或误创建另一个同 ID 文档位置。

## Routing 的收益和风险

按商家 ID 路由能加速单商家商品查询，但头部商家可能把一个 shard 写热、存储打满。
按用户 ID 路由订单也会让超大企业客户形成热点。应先分析 key 分布、单 key 上限和查询模式，
再决定是否采用 routing partition 或更细粒度键。

shard 数也不是越多越好：更多 shard 增加集群状态、线程调度和聚合归并成本；过少又限制并行度和单 shard 上限。容量设计要按文档量、写入率、查询 fan-out、segment 和故障恢复时间联合测算。

## 查询优化路径

- 过滤、排序和聚合字段使用正确 mapping，避免动态 mapping 把数值当字符串。
- 返回大结果集使用 PIT + `search_after`，避免深 `from + size` 在每个 shard 保留大量候选。
- 聚合前用高选择性 filter 缩小文档集合，并限制用户可控 bucket 数。
- 用 profile API 定位慢在 query、collector 还是 fetch，但只在诊断时开启以免额外开销。

在 eMall 中，商品搜索以类目、品牌、价格的 keyword/数值 Doc Values 做筛选聚合；商家后台查询可评估商家 routing，但必须给头部商家设计拆分策略和热点告警。

参考：[OpenSearch Routing](https://docs.opensearch.org/latest/mappings/metadata-fields/routing/)

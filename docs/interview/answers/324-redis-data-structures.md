# 324 Redis 常用数据结构有哪些？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Redis 常用数据结构包括 String、Hash、List、Set、Sorted Set、Bitmap、HyperLogLog、Stream 和
Geospatial。不同结构不是语法差异，而是适合不同访问模式。

设计 Redis 缓存时要先看读写模式、数据大小、过期策略和一致性要求，再选结构。

## 基础结构

常见结构：

- String：字符串、数字、JSON、计数器。
- Hash：对象字段集合。
- List：队列、最近列表。
- Set：去重集合、标签集合。
- Sorted Set：排行榜、延迟任务候选集。

这些结构覆盖大多数业务缓存场景。

## 扩展结构

扩展结构包括：

- Bitmap：签到、状态位。
- HyperLogLog：UV 近似统计。
- Stream：轻量消息流。
- Geospatial：地理位置计算。

扩展结构能节省内存或简化特定场景。

## 选型原则

原则：

- 单值缓存优先 String。
- 对象局部字段更新可用 Hash。
- 去重用 Set。
- 排序和权重用 Sorted Set。
- 大规模布尔状态用 Bitmap。
- 近似去重统计用 HyperLogLog。
- 可靠消息优先 Kafka，Stream 适合轻量场景。

不要为了炫技选择复杂结构。

## 在 eMall 项目中怎么讲？

eMall 中商品详情缓存可以用 String 保存聚合 JSON。

购物车可以用 Hash，以 `skuId` 为 field，数量和选中状态作为 value。排行榜或热销榜可以用
Sorted Set，以销量作为 score。用户是否领取优惠券可以用 Set 或 Bitmap。

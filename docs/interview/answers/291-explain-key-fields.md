# 291 `explain` 重点看哪些字段？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`EXPLAIN` 要重点看访问类型、使用的索引、扫描行数、过滤比例、排序和临时表。
常看的字段包括 `type`、`possible_keys`、`key`、`rows`、`filtered`、`Extra`，以及 MySQL 8 的 `EXPLAIN ANALYZE` 实际耗时。

它用于判断 SQL 是否走了合理索引，以及是否存在大扫描、回表、排序和临时表。

## type

`type` 表示访问方式，常见从好到差大致是：

- `const`。
- `eq_ref`。
- `ref`。
- `range`。
- `index`。
- `ALL`。

`ALL` 通常表示全表扫描，要重点关注。

## key 和 rows

`possible_keys` 表示可能使用的索引。

`key` 表示实际使用的索引。

`rows` 表示优化器估算扫描行数。`rows` 很大说明查询可能扫描太多数据。

## Extra

`Extra` 中要关注：

- `Using index`。
- `Using where`。
- `Using filesort`。
- `Using temporary`。
- `Using index condition`。

`Using filesort` 和 `Using temporary` 不一定绝对坏，但在高频大数据查询中要警惕。

## 电商系统实践

订单后台查询慢时，用 `EXPLAIN` 看是否走了按商家、状态和时间范围设计的联合索引。

如果 `type=ALL` 且 `rows` 很大，说明可能在扫订单大表。

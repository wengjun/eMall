# 294 如何设计订单表索引？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

如何设计订单表索引？

## 先给面试官的短答案

订单表索引要围绕核心访问路径设计，而不是给所有字段建索引。
常见访问路径包括按订单 ID 查详情、按用户查订单列表、按商家查订单、按状态和时间扫描、按支付单或订单号定位。

订单表通常需要主键、业务唯一键、用户维度、商家维度和状态时间维度索引。

## 核心查询

常见查询：

- 用户查看订单列表。
- 用户查看订单详情。
- 商家查看订单列表。
- 后台按状态处理订单。
- 支付回调按订单号定位。
- 对账按时间范围扫描。

索引要服务这些明确路径。

## 示例索引

可能设计：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_order_no (order_no)
KEY idx_user_created (user_id, created_at, id)
KEY idx_merchant_status_created (merchant_id, status, created_at, id)
KEY idx_status_created (status, created_at, id)
```

实际字段要结合分库分表策略和业务查询调整。

## 注意事项

注意：

- 避免过多单列索引。
- 列表查询尽量覆盖常用字段。
- 深分页要用游标。
- 后台复杂报表不要压主库。
- 历史订单可以归档。

订单表增长很快，索引成本会持续放大。

## 在 eMall 项目中怎么讲？

用户订单列表按 `user_id + created_at` 查，商家后台按 `merchant_id + status + created_at` 查。

对账任务按状态和时间范围扫描，并限制批次大小。

## 深度增强：索引设计图

![索引设计从访问路径出发](../../assets/index-design.svg)

订单表索引要从访问路径反推，不能看到字段就建索引。订单是高写入表，索引越多，写入成本、页分裂、
Buffer Pool 压力和变更风险越高。

## 深度增强：典型 SQL 和索引

用户订单列表：

```sql
SELECT order_no, status, total_amount, created_at
FROM orders
WHERE user_id = ?
  AND created_at < ?
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

适合索引：

```sql
CREATE INDEX idx_user_created_id
ON orders (user_id, created_at, id);
```

商家后台按状态查：

```sql
SELECT order_no, user_id, status, total_amount, created_at
FROM orders
WHERE merchant_id = ?
  AND status = ?
  AND created_at >= ?
  AND created_at < ?
ORDER BY created_at DESC, id DESC
LIMIT 100;
```

适合索引：

```sql
CREATE INDEX idx_merchant_status_created_id
ON orders (merchant_id, status, created_at, id);
```

## 深度增强：生产边界

- 深分页不要用大 offset，改用游标或 `created_at + id` 翻页。
- 支付回调要通过唯一订单号或支付单号快速定位。
- 低频报表不要压订单主库，走数仓或只读库。
- 索引字段顺序要匹配等值、范围和排序。
- 每个新增索引都要评估写入放大和磁盘成本。

## 深度增强：面试高分表达

```text
我会先列访问路径，而不是先列字段。用户列表、商家列表、支付回调、补偿扫描是不同查询模型。
订单号要唯一，用户列表用 user_id + created_at + id，商家后台用 merchant_id + status + created_at。
复杂报表和历史查询不应该走交易主库，历史订单要归档，深分页要改游标。
```

## 专家级完整回答

```text
订单表索引要按访问路径设计。主键用于详情查询，订单号做唯一键，用户订单列表用 user_id + created_at，
商家后台用 merchant_id + status + created_at，对账和补偿用 status + created_at。

同时要控制索引数量，避免低频复杂查询拖垮写入。历史订单归档，深分页用游标，报表查询走数仓。
```

## 回答评分点

高分答案应该覆盖：

- 订单索引要围绕访问路径。
- 用户、商家、状态时间是常见维度。
- 订单号需要唯一约束。
- 避免过多单列索引。
- 历史和报表查询要分流。

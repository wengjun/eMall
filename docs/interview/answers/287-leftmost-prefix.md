# 287 联合索引最左前缀是什么？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

最左前缀是指联合索引按字段顺序组织，查询条件必须从索引最左侧字段开始连续使用，才能充分利用索引。
例如索引 `(user_id, status, created_at)` 可以支持按 `user_id`，或 `user_id + status` 查询。

跳过最左字段通常无法有效利用该联合索引。

## 示例

索引：

```sql
CREATE INDEX idx_order
ON orders(user_id, status, created_at);
```

可以较好使用：

```sql
WHERE user_id = ?
WHERE user_id = ? AND status = ?
```

不适合单独使用：

```sql
WHERE status = ?
WHERE created_at > ?
```

因为跳过了 `user_id`。

## 范围条件影响

联合索引遇到范围条件后，后续字段通常不能继续用于精确定位。

例如：

```sql
WHERE user_id = ?
  AND created_at > ?
  AND status = ?
```

如果索引顺序不合理，`status` 可能无法充分利用。

## 设计原则

设计时考虑：

- 等值条件靠前。
- 高选择性字段靠前。
- 排序字段和范围字段位置。
- 高频查询优先。
- 不为低频组合滥建索引。

索引字段顺序非常重要。

## 电商系统实践

用户订单列表常按 `user_id` 查询，再按 `created_at` 排序。

索引可以设计为 `(user_id, created_at, id)`，而不是把不常用字段放在最左侧。

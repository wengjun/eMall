# 172 游标分页和 offset 分页如何取舍？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`offset` 分页适合数据量小、需要跳页、对实时一致性要求不高的场景。
游标分页适合大数据量、连续向后翻页、要求性能稳定的场景。

电商核心系统中，商品流、订单列表、消息列表更适合游标分页；后台简单配置列表可以使用 `offset`。

## offset 分页

示例：

```sql
SELECT *
FROM product
ORDER BY id DESC
LIMIT 20 OFFSET 40;
```

优点：

- 实现简单。
- 支持跳到任意页。
- 前端页码组件容易适配。

缺点：

- 深分页性能差。
- 数据新增或删除后，结果可能重复或遗漏。
- 页码越深，查询成本越高。

## 游标分页

示例：

```sql
SELECT *
FROM product
WHERE id < ?
ORDER BY id DESC
LIMIT 20;
```

优点：

- 性能稳定。
- 可以利用索引范围扫描。
- 更适合无限滚动和大数据列表。
- 数据变化时更不容易出现重复。

缺点：

- 不适合任意跳页。
- 游标设计需要包含排序字段。
- 多字段排序时实现更复杂。

## 游标怎么设计？

游标应能唯一定位上一页的边界。

如果按 `created_at DESC, id DESC` 排序，游标要包含 `created_at` 和 `id`：

```sql
SELECT *
FROM orders
WHERE user_id = ?
  AND (
      created_at < ?
      OR (created_at = ? AND id < ?)
  )
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

只用时间做游标可能重复或漏数据，因为同一毫秒可能有多条记录。

## 在 eMall 项目中怎么讲？

用户订单列表可以返回 `nextCursor`：

```json
{
    "items": [],
    "nextCursor": "eyJjcmVhdGVkQXQiOiIyMDI2LTA0LTMwVDEwOjAwOjAwWiIsImlkIjoiOTkifQ=="
}
```

客户端下一次带上 `nextCursor`，服务端解析后执行范围查询。

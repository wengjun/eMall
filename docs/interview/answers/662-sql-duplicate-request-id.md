# 662 手写 SQL 找出重复 request_id。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```sql
select request_id,
       count(*) as duplicate_count
from order_request
where request_id is not null
group by request_id
having count(*) > 1
order by duplicate_count desc;
```

### 必测用例

- 同一 request ID 出现一次、两次和多次时分组计数正确。
- 明确 `NULL`、空字符串、不同调用方和不同时间窗口是否视为同一键。
- 在大表上用执行计划验证组合索引能支持筛选与分组。

### 生产化差异

- 重复检查只能用于发现历史问题，在线防重仍要依靠正确范围的唯一约束。
- 按时间或租户分批扫描并设置保留期，避免全表聚合影响主库。

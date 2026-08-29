# 660 手写 SQL 查询最近 30 天每天下单量。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```sql
select date(created_at) as order_day,
       count(*) as order_count
from order_main
where created_at >= current_date - interval '30' day
group by date(created_at)
order by order_day;
```

### 必测用例

- 覆盖日期边界、跨月、闰日、空日期和一天内多笔订单。
- 明确业务时区，验证 UTC 存储在夏令时地区映射到业务日期的结果。
- 说明是否需要补齐零订单日期，以及取消订单是否计入。

### 生产化差异

- 使用半开时间范围和可用索引，避免在索引列上直接套函数导致全表扫描。
- 报表查询应从只读模型或数仓执行；补零可连接日历表。

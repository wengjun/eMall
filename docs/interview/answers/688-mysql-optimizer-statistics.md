# 688 MySQL 优化器如何使用统计信息选择执行计划？

[返回按分类学习面试题](../README.md)

## 优化器在估算什么

MySQL 优化器枚举可行的访问路径和 join 顺序，用成本模型估算读取页数、返回行数、CPU 比较和临时操作成本，再选择估计成本较低的计划。核心输入是基数估计；行数估错几个数量级，后续 join 算法和顺序通常都会错。

统计来源包括表行数、索引基数、索引页信息和列 Histogram。InnoDB 持久统计通常通过采样得到，不是每次都精确扫描全表。

## 为什么统计会误导

- 数据倾斜：`status='PAID'` 与 `status='FAILED'` 分布差异巨大，平均选择率不适用。
- 列相关：城市和邮编高度相关，独立性假设会低估或高估组合条件。
- 参数差异：同一预编译形状对热门商家和普通商家的最优计划不同。
- 统计过期：大批导入或删除后，样本尚未反映新分布。
- 复杂表达式、隐式类型转换或函数包裹索引列，让可用索引消失。

Histogram 能改善无索引列或倾斜列的分布估计，但增加维护成本，也不能自动表达任意多列相关性。

## 正确排查顺序

```sql
EXPLAIN ANALYZE
SELECT order_id, paid_at
FROM orders
WHERE merchant_id = 42 AND status = 'PAID'
ORDER BY paid_at DESC
LIMIT 50;
```

重点比较每个节点的 estimated rows 与 actual rows、loops、实际耗时和是否产生排序/临时表。若第一处基数已经偏离百倍，后续耗时只是结果，不应先用 hint 强行固定末端计划。

然后检查谓词类型、复合索引列顺序、统计更新时间和数据分布；必要时 `ANALYZE TABLE`、建立 Histogram 或设计覆盖索引。可先用 invisible index 验证删除索引的影响。

## 索引不是越多越好

复合索引应匹配高频访问模式，并考虑等值列、范围列、排序和覆盖需求。每个二级索引都会增加写放大、Buffer Pool 占用、页分裂和 DDL 成本。低频报表不应拖累核心订单写路径，可转移到分析库。

## 计划治理

生产应保存关键 SQL 的 fingerprint、执行计划、P95/P99、扫描行数和返回行数，在发布或统计变化前后做基线比较。Optimizer hint 只能作为验证或紧急止血，长期应修复统计、SQL 或索引设计，并设置可回滚方案。

参考：[MySQL 8.4 Optimizer Statistics](https://dev.mysql.com/doc/refman/8.4/en/controlling-optimizer.html)

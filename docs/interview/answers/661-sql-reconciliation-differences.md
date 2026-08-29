# 661 手写 SQL 查询支付对账差异。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```sql
select p.payment_id,
       p.channel_trade_no,
       p.amount as local_amount,
       c.amount as channel_amount,
       p.status as local_status,
       c.status as channel_status
from payment_order p
join channel_bill c on c.channel_trade_no = p.channel_trade_no
where p.amount <> c.amount
   or p.status <> c.status;
```

### 必测用例

- 覆盖仅渠道存在、仅本地存在、金额不同、状态不同和完全一致。
- 验证重复渠道流水、空值、币种和小数精度不会造成误判。
- 按水位分批运行时，边界记录不丢失且重复批次结果幂等。

### 生产化差异

- MySQL 没有原生 FULL OUTER JOIN 时要正确组合左右差异，并为业务键和时间范围建索引。
- 修复动作与查询分离，差异记录需审计、可重跑并设置人工审批阈值。

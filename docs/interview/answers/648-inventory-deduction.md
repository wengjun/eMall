# 648 手写库存条件扣减逻辑。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```sql
update inventory_stock
set available_quantity = available_quantity - :quantity,
    reserved_quantity = reserved_quantity + :quantity
where sku_id = :skuId
  and available_quantity >= :quantity;
```

```java
if (updatedRows != 1) {
    throw new BusinessException("inventory not enough");
}
```

### 必测用例

- 库存充足时正确扣减，库存为零或请求数量超过可售量时拒绝且不出现负数。
- 同一订单重复扣减只能成功一次，不同订单并发扣减总量不能超过初始库存。
- 扣减与释放并发时验证状态和数量守恒。

### 生产化差异

- 使用数据库条件更新或原子脚本，并记录订单维度的预占流水实现幂等。
- 热门 SKU 还需热点隔离、限购和对账，不能只依赖单行锁。

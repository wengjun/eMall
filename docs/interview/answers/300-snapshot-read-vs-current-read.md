# 300 快照读和当前读有什么区别？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

快照读读取事务可见的历史版本，通常不加锁，依赖 MVCC。
当前读读取最新已提交并可加锁的版本，用于更新、删除、插入和 `select for update` 等需要参与并发控制的操作。

普通查询多是快照读，写操作和加锁查询是当前读。

## 快照读

典型语句：

```sql
SELECT * FROM orders WHERE id = ?;
```

在 InnoDB 中普通 `SELECT` 通常是快照读。

它读取 Read View 可见版本，不阻塞其他事务写入。

## 当前读

典型语句：

```sql
SELECT * FROM orders WHERE id = ? FOR UPDATE;
UPDATE orders SET status = ? WHERE id = ?;
DELETE FROM orders WHERE id = ?;
```

当前读读取最新版本，并可能加记录锁、间隙锁或 next-key lock。

## 为什么重要？

如果用快照读判断库存：

```sql
SELECT available FROM stock WHERE sku_id = ?;
```

读到的库存可能在并发下已经变化。

库存扣减应使用当前读或条件更新。

## 电商系统实践

用户订单列表使用快照读即可，避免阻塞订单更新。

库存扣减必须用条件更新：

```sql
UPDATE sku_inventory
SET available = available - :quantity
WHERE sku_id = :sku_id
  AND available >= :quantity;
```

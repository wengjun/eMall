# 126 事务隔离级别如何配置？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Spring 可以通过 `@Transactional(isolation = Isolation.xxx)` 配置隔离级别，例如 `READ_COMMITTED`、
`REPEATABLE_READ`、`SERIALIZABLE`。隔离级别决定脏读、不可重复读、幻读和锁竞争的权衡。
实际效果还取决于数据库实现，例如 MySQL InnoDB 默认通常是 `REPEATABLE_READ`。

生产中不要盲目提高隔离级别，要结合一致性需求和性能成本。

## 常见隔离级别

常见级别：

- `READ_UNCOMMITTED`。
- `READ_COMMITTED`。
- `REPEATABLE_READ`。
- `SERIALIZABLE`。
- `DEFAULT`。

`DEFAULT` 表示使用数据库默认隔离级别。

## Spring 配置方式

示例：

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void createOrder() {
}
```

也可以在数据库连接池或数据库层设置默认隔离级别。

项目要避免同一服务中隔离级别混乱。

## 隔离级别解决什么？

主要问题：

- 脏读：读到未提交数据。
- 不可重复读：同一事务两次读同一行结果不同。
- 幻读：同一事务两次范围查询结果集不同。

隔离越高，一致性越强，但并发性能和锁冲突成本通常越高。

## 数据库差异

不同数据库实现不同。

例如 MySQL InnoDB 的 `REPEATABLE_READ` 通过 MVCC 和锁机制处理很多场景。

PostgreSQL 的隔离语义也有自己的实现细节。

面试中要说明：Spring 只是传递隔离级别，最终行为由数据库决定。

## 不要滥用 SERIALIZABLE

`SERIALIZABLE` 一致性最强，但并发成本高。

在高并发电商系统中，盲目使用可能导致：

- 锁等待。
- 死锁。
- 吞吐下降。
- P99 升高。

通常优先用业务约束、唯一键、乐观锁和条件更新解决具体一致性问题。

## 电商系统实践

库存扣减不一定靠提高隔离级别解决。

更常见是：

```sql
update inventory
set available = available - ?
where sku_id = ? and available >= ?
```

用条件更新保证不超卖。

隔离级别只是事务一致性工具之一，不替代业务并发设计。

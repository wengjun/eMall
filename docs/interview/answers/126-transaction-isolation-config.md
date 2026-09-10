# 126 事务隔离级别如何配置？

[返回按分类学习面试题](../README.md)

## 核心结论

Spring 可以通过 `@Transactional(isolation = Isolation.xxx)` 配置隔离级别，例如 `READ_COMMITTED`、
`REPEATABLE_READ`、`SERIALIZABLE`。隔离级别决定脏读、不可重复读、幻读和锁竞争的权衡。
实际效果还取决于数据库实现，例如 MySQL InnoDB 默认通常是 `REPEATABLE_READ`。

生产中不要盲目提高隔离级别，要结合一致性需求和性能成本。

## Spring 配置方式

示例：

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void createOrder() {
}
```

也可以在数据库连接池或数据库层设置默认隔离级别。

项目要避免同一服务中隔离级别混乱。

## 数据库差异

不同数据库实现不同。

例如 MySQL InnoDB 的 `REPEATABLE_READ` 通过 MVCC 和锁机制处理很多场景。

PostgreSQL 的隔离语义也有自己的实现细节。

面试中要说明：Spring 只是传递隔离级别，最终行为由数据库决定。

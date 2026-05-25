# 300 快照读和当前读有什么区别？

[返回按分类学习面试题](../README.md)

## 题目

快照读和当前读有什么区别？

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

## 在 eMall 项目中怎么讲？

用户订单列表使用快照读即可，避免阻塞订单更新。

库存扣减必须用条件更新：

```sql
UPDATE stock
SET available = available - 1
WHERE sku_id = ?
  AND available > 0;
```

## 深度增强：数据访问和扩展图

![数据库、缓存和消息一致性链路](../assets/data-cache-mq.svg)

数据库题要从访问路径、索引、锁、事务和容量出发。电商系统的数据层既要支撑高并发读写，
又要保证订单、库存、支付等事实数据可追踪。缓存和消息可以提升性能，但不能替代数据库事实来源。

## 深度增强：Java 17 数据访问策略示例

```java
record QueryPlan(String accessPath, boolean usesIndex, boolean requiresPagination) {

    boolean safeForOnlineTraffic() {
        return usesIndex && requiresPagination;
    }
}

final class OnlineQueryPolicy {

    void verify(QueryPlan plan) {
        if (!plan.safeForOnlineTraffic()) {
            throw new IllegalArgumentException("Online query must use index and pagination");
        }
    }
}
```

这段代码体现线上查询治理：不是 SQL 能跑就可以上线，而是要确认走索引、可分页、可限流、可观测。

## 深度增强：生产边界

核心表设计要从典型查询倒推索引，避免全表扫描、深分页和大事务。分库分表要先选好分片键，
避免跨分片事务和热点分片。任何数据迁移都要支持灰度、校验、回滚或修复。

## 深度增强：面试高分表达

我会从访问模式回答数据题：谁查、按什么条件查、QPS 多少、数据量多大、是否强一致、是否需要分页和排序。
然后再决定索引、分片、缓存、读写分离和归档策略。

## 专家级完整回答

```text
快照读读取 MVCC 可见的历史版本，普通 SELECT 通常是快照读，不加锁且并发性能好。
当前读读取最新版本并参与锁控制，UPDATE、DELETE、INSERT 和 SELECT FOR UPDATE 都是当前读。

读列表可以用快照读；库存扣减、状态流转等关键写入必须用当前读或条件更新保证并发正确性。
```

## 回答评分点

高分答案应该覆盖：

- 快照读依赖 MVCC。
- 当前读读取最新并可能加锁。
- 普通 SELECT 通常是快照读。
- 更新和 `FOR UPDATE` 是当前读。
- 库存扣减不能只靠快照读。

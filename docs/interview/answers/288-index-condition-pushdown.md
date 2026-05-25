# 288 索引下推是什么？

[返回按分类学习面试题](../README.md)

## 题目

索引下推是什么？

## 先给面试官的短答案

索引下推是 MySQL 在使用二级索引扫描时，把部分 `WHERE` 条件下推到存储引擎层，在回表前先用索引中的字段过滤数据。
这样可以减少回表次数，提高查询性能。

它的价值是让存储引擎更早过滤无效记录。

## 没有索引下推

没有索引下推时：

- 存储引擎按索引找到记录。
- 回表读取整行。
- Server 层判断其他条件。

如果很多记录回表后才被过滤，成本会很高。

## 有索引下推

有索引下推时：

- 存储引擎扫描索引。
- 使用索引中已有字段判断部分条件。
- 不满足条件的记录不回表。
- 满足条件的记录再回表。

减少了不必要的回表。

## 示例

索引：

```sql
CREATE INDEX idx_user_status ON orders(user_id, status);
```

查询：

```sql
SELECT *
FROM orders
WHERE user_id = ?
  AND status = 'PAID';
```

存储引擎可以在索引层先过滤 `status`。

## 在 eMall 项目中怎么讲？

订单查询如果联合索引中包含 `user_id` 和 `status`，数据库可以在索引扫描阶段过滤订单状态。

这样比把大量用户订单全部回表后再过滤更高效。

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
索引下推是把可以依赖索引字段判断的条件下推到存储引擎层，在回表前过滤记录。
它能减少二级索引查询的回表次数，尤其适合联合索引中包含过滤字段但不能完全覆盖查询的场景。

它不是替代合理索引设计，而是优化执行过程的一种能力。
```

## 回答评分点

高分答案应该覆盖：

- 索引下推发生在存储引擎层。
- 它在回表前过滤。
- 作用是减少回表次数。
- 依赖索引中已有字段。
- 不能替代合理索引设计。

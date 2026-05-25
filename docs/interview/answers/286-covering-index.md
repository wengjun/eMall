# 286 覆盖索引是什么？

[返回按分类学习面试题](../README.md)

## 题目

覆盖索引是什么？

## 先给面试官的短答案

覆盖索引是指查询所需字段都能从某个索引中直接获得，不需要回表读取整行。
它能减少随机 IO，提高高频查询性能，尤其适合列表页、只读查询和分页查询。

覆盖索引不是一种特殊索引，而是一种查询命中索引的效果。

## 示例

索引：

```sql
CREATE INDEX idx_user_order
ON orders(user_id, created_at, id, status, amount);
```

查询：

```sql
SELECT id, status, amount, created_at
FROM orders
WHERE user_id = ?
ORDER BY created_at DESC
LIMIT 20;
```

查询字段都在索引里，就可以避免回表。

## 优点

优点：

- 减少回表。
- 降低随机 IO。
- 提高列表查询性能。
- 对分页和排序友好。

对读多写少的高频接口很有价值。

## 代价

代价：

- 索引更大。
- 写入更慢。
- 占用更多存储。
- 维护成本更高。

不能为了覆盖所有查询创建过宽索引。

## 在 eMall 项目中怎么讲？

用户订单列表是高频接口，可以让索引覆盖列表展示字段。

但订单详情字段多，不应为了覆盖详情页把大量字段都塞进索引。

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
覆盖索引是查询所需字段都包含在索引中，数据库可以直接从索引返回结果，不需要回表。
它能降低随机 IO，适合高频列表、分页和只读查询。

但覆盖索引会增加索引体积和写入成本，所以要针对核心高频查询设计，不能无节制增加索引列。
```

## 回答评分点

高分答案应该覆盖：

- 覆盖索引避免回表。
- 它是查询效果，不是单独索引类型。
- 适合高频列表查询。
- 会增加索引大小和写入成本。
- 不应创建过宽索引。

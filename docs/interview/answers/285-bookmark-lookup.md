# 285 回表是什么？

[返回按分类学习面试题](../README.md)

## 题目

回表是什么？

## 先给面试官的短答案

回表是指通过二级索引找到主键后，再通过主键索引查询整行数据。
因为 InnoDB 二级索引叶子节点不存整行，只存索引列和主键，所以查询非索引列时需要回到聚簇索引。

回表次数多会增加随机 IO 和查询延迟。

## 示例

索引：

```sql
CREATE INDEX idx_user_id ON orders(user_id);
```

查询：

```sql
SELECT order_no, amount, status
FROM orders
WHERE user_id = ?;
```

如果 `order_no, amount, status` 不在二级索引里，就需要回表读取。

## 回表成本

成本来自：

- 多一次 B+Tree 查询。
- 随机访问聚簇索引。
- 返回行越多，回表越多。
- 缓存未命中时 IO 更明显。

小结果集回表通常可接受，大结果集大量回表会变慢。

## 如何减少回表？

方法：

- 使用覆盖索引。
- 只查询必要字段。
- 控制返回行数。
- 优化联合索引。
- 避免低选择性索引返回大量行。

不要为了避免回表无限增加索引列。

## 在 eMall 项目中怎么讲？

用户订单列表如果只展示 `order_id, status, created_at, amount`，
可以设计覆盖索引减少回表。

订单详情页需要完整字段，按主键查询即可。

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
回表是二级索引查询的第二步。InnoDB 二级索引叶子节点保存索引列和主键值，
如果查询字段不在二级索引中，需要用主键再查聚簇索引拿到整行。

大量回表会增加随机 IO 和延迟。可以通过覆盖索引、减少查询字段和控制结果集降低回表成本。
```

## 回答评分点

高分答案应该覆盖：

- 回表发生在二级索引查询后。
- 二级索引叶子节点不存整行。
- 回表通过主键查聚簇索引。
- 大量回表会变慢。
- 覆盖索引可以减少回表。

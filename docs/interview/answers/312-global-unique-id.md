# 312 全局唯一 ID 如何生成？

[返回按分类学习面试题](../README.md)

## 题目

全局唯一 ID 如何生成？

## 先给面试官的短答案

全局唯一 ID 常见方案有数据库自增、号段模式、UUID、Snowflake、Redis 原子递增和专用发号服务。
大型电商常用 Snowflake 或号段服务，因为它们能在分布式环境下生成趋势递增、可路由、性能较高的 ID。

选择方案要看唯一性、趋势性、性能、可用性、可读性和是否需要携带业务路由。

## 常见方案

方案包括：

- 数据库自增 ID。
- UUID。
- Snowflake。
- 号段模式。
- Redis `INCR`。
- 专用 ID 服务。

每种方案都有成本。

## 选择标准

关注：

- 全局唯一。
- 高并发性能。
- 趋势递增。
- 可排序。
- 可读性。
- 可用性。
- 时钟依赖。
- 是否能包含分片信息。

订单 ID 通常还要考虑按时间排序和分片路由。

## 号段模式

号段模式从数据库批量申请一段 ID。

优点：

- 数据库压力低。
- 本地生成性能高。
- 可控且稳定。

缺点：

- 可能有 ID 空洞。
- 发号服务要高可用。

## 在 eMall 项目中怎么讲？

订单 ID 可以使用 Snowflake 或号段服务生成，保证高并发下全局唯一和趋势递增。

如果订单库分片，还可以在 ID 中加入分片位，提升路由效率。

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
全局唯一 ID 方案包括数据库自增、UUID、Snowflake、Redis INCR、号段模式和发号服务。
大型电商订单通常需要唯一、高性能、趋势递增、可排序，并最好携带分片路由信息。

我会优先考虑 Snowflake 或号段服务，同时处理时钟回拨、机器号分配、服务高可用和 ID 空洞问题。
```

## 回答评分点

高分答案应该覆盖：

- 能列出常见 ID 方案。
- 知道各自取舍。
- 订单 ID 需要高性能和趋势递增。
- Snowflake 和号段常见。
- 要考虑时钟、可用性和分片路由。

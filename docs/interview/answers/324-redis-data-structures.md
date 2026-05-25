# 324 Redis 常用数据结构有哪些？

[返回按分类学习面试题](../README.md)

## 题目

Redis 常用数据结构有哪些？

## 先给面试官的短答案

Redis 常用数据结构包括 String、Hash、List、Set、Sorted Set、Bitmap、HyperLogLog、Stream 和
Geospatial。不同结构不是语法差异，而是适合不同访问模式。

设计 Redis 缓存时要先看读写模式、数据大小、过期策略和一致性要求，再选结构。

## 基础结构

常见结构：

- String：字符串、数字、JSON、计数器。
- Hash：对象字段集合。
- List：队列、最近列表。
- Set：去重集合、标签集合。
- Sorted Set：排行榜、延迟任务候选集。

这些结构覆盖大多数业务缓存场景。

## 扩展结构

扩展结构包括：

- Bitmap：签到、状态位。
- HyperLogLog：UV 近似统计。
- Stream：轻量消息流。
- Geospatial：地理位置计算。

扩展结构能节省内存或简化特定场景。

## 选型原则

原则：

- 单值缓存优先 String。
- 对象局部字段更新可用 Hash。
- 去重用 Set。
- 排序和权重用 Sorted Set。
- 大规模布尔状态用 Bitmap。
- 近似去重统计用 HyperLogLog。
- 可靠消息优先 Kafka，Stream 适合轻量场景。

不要为了炫技选择复杂结构。

## 在 eMall 项目中怎么讲？

eMall 中商品详情缓存可以用 String 保存聚合 JSON。

购物车可以用 Hash，以 `skuId` 为 field，数量和选中状态作为 value。排行榜或热销榜可以用
Sorted Set，以销量作为 score。用户是否领取优惠券可以用 Set 或 Bitmap。

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
Redis 不只是 key-value，它提供了多种数据结构。String 适合单值和计数，Hash 适合对象字段，
Set 适合去重，Sorted Set 适合排名，Bitmap 适合大规模状态位，HyperLogLog 适合近似 UV。

生产设计时要从访问模式出发，而不是只看数据长什么样。还要考虑 key 数量、value 大小、TTL、
一致性和热点风险。
```

## 回答评分点

高分答案应该覆盖：

- 能列出常用结构。
- 能说明不同结构的使用场景。
- 能结合商品、购物车、排行榜举例。
- 能提到内存和访问模式。
- 不把 Redis 只理解成简单字符串缓存。

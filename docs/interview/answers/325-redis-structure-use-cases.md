# 325 String、Hash、List、Set、ZSet 分别适合什么场景？

[返回按分类学习面试题](../README.md)

## 题目

String、Hash、List、Set、ZSet 分别适合什么场景？

## 先给面试官的短答案

String 适合单值缓存和计数器，Hash 适合对象字段，List 适合顺序队列，Set 适合去重集合，
ZSet 适合带分数排序的集合。

它们的核心区别是访问模式：是否按字段访问、是否要求顺序、是否要求去重、是否要求排序。

## String

适合：

- 商品详情 JSON。
- 登录 token。
- 分布式锁 value。
- 库存预热值。
- 计数器。

String 简单直接，但大 JSON 局部更新成本较高。

## Hash

适合：

- 购物车。
- 用户资料字段。
- 商品基础属性。
- 配置对象。

Hash 适合字段级读写，但字段过多也会形成大 key。

## List

适合：

- 最近浏览。
- 简单队列。
- 时间顺序列表。

如果需要可靠消息、重试、消费组和堆积治理，应该优先选择 Kafka。

## Set 和 ZSet

Set 适合：

- 用户标签。
- 已领取优惠券集合。
- 去重任务集合。
- 共同关注计算。

ZSet 适合：

- 商品热销榜。
- 搜索热词榜。
- 延迟任务候选池。
- 带时间戳排序的记录。

## 在 eMall 项目中怎么讲？

商品详情页可以用 String 缓存聚合结果。购物车更适合 Hash，因为经常按 SKU 修改数量。

优惠券领取去重可以用 Set。热销榜可以用 ZSet，用销量或综合分作为 score。

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
Redis 结构选型要看访问模式。String 适合整体读写，Hash 适合对象字段读写，List 适合顺序追加
和弹出，Set 适合无序去重，ZSet 适合去重加排序。

在电商系统里，商品详情适合 String，购物车适合 Hash，优惠券领取适合 Set，排行榜适合 ZSet。
结构选错会带来内存浪费、大 key、复杂度上升和性能风险。
```

## 回答评分点

高分答案应该覆盖：

- 说清五种结构的核心差异。
- 能用访问模式做选择。
- 能给出电商场景例子。
- 能提到大 key 和内存风险。
- 能区分 List 和专业消息队列。

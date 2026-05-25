# 287 联合索引最左前缀是什么？

[返回按分类学习面试题](../README.md)

## 题目

联合索引最左前缀是什么？

## 先给面试官的短答案

最左前缀是指联合索引按字段顺序组织，查询条件必须从索引最左侧字段开始连续使用，才能充分利用索引。
例如索引 `(user_id, status, created_at)` 可以支持按 `user_id`，或 `user_id + status` 查询。

跳过最左字段通常无法有效利用该联合索引。

## 示例

索引：

```sql
CREATE INDEX idx_order
ON orders(user_id, status, created_at);
```

可以较好使用：

```sql
WHERE user_id = ?
WHERE user_id = ? AND status = ?
```

不适合单独使用：

```sql
WHERE status = ?
WHERE created_at > ?
```

因为跳过了 `user_id`。

## 范围条件影响

联合索引遇到范围条件后，后续字段通常不能继续用于精确定位。

例如：

```sql
WHERE user_id = ?
  AND created_at > ?
  AND status = ?
```

如果索引顺序不合理，`status` 可能无法充分利用。

## 设计原则

设计时考虑：

- 等值条件靠前。
- 高选择性字段靠前。
- 排序字段和范围字段位置。
- 高频查询优先。
- 不为低频组合滥建索引。

索引字段顺序非常重要。

## 在 eMall 项目中怎么讲？

用户订单列表常按 `user_id` 查询，再按 `created_at` 排序。

索引可以设计为 `(user_id, created_at, id)`，而不是把不常用字段放在最左侧。

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
联合索引按字段顺序构建，使用时遵循最左前缀原则。查询条件从最左字段开始连续匹配时，
索引利用效果最好；如果跳过最左字段，通常无法有效使用该索引。

设计联合索引要结合等值条件、范围条件、排序和查询频率，字段顺序会直接影响执行计划。
```

## 回答评分点

高分答案应该覆盖：

- 联合索引有字段顺序。
- 查询要从最左字段开始匹配。
- 跳过最左字段通常效果差。
- 范围条件会影响后续字段利用。
- 索引顺序要按查询模式设计。

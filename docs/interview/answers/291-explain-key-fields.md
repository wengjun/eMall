# 291 `explain` 重点看哪些字段？

[返回按分类学习面试题](../README.md)

## 题目

`explain` 重点看哪些字段？

## 先给面试官的短答案

`EXPLAIN` 要重点看访问类型、使用的索引、扫描行数、过滤比例、排序和临时表。
常看的字段包括 `type`、`possible_keys`、`key`、`rows`、`filtered`、`Extra`，以及 MySQL 8 的 `EXPLAIN ANALYZE` 实际耗时。

它用于判断 SQL 是否走了合理索引，以及是否存在大扫描、回表、排序和临时表。

## type

`type` 表示访问方式，常见从好到差大致是：

- `const`。
- `eq_ref`。
- `ref`。
- `range`。
- `index`。
- `ALL`。

`ALL` 通常表示全表扫描，要重点关注。

## key 和 rows

`possible_keys` 表示可能使用的索引。

`key` 表示实际使用的索引。

`rows` 表示优化器估算扫描行数。`rows` 很大说明查询可能扫描太多数据。

## Extra

`Extra` 中要关注：

- `Using index`。
- `Using where`。
- `Using filesort`。
- `Using temporary`。
- `Using index condition`。

`Using filesort` 和 `Using temporary` 不一定绝对坏，但在高频大数据查询中要警惕。

## 在 eMall 项目中怎么讲？

订单后台查询慢时，用 `EXPLAIN` 看是否走了按商家、状态和时间范围设计的联合索引。

如果 `type=ALL` 且 `rows` 很大，说明可能在扫订单大表。

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
EXPLAIN 重点看 type、possible_keys、key、rows、filtered 和 Extra。
type 判断访问方式，key 看实际索引，rows 看扫描量，Extra 看是否覆盖索引、索引下推、文件排序和临时表。

MySQL 8 可以用 EXPLAIN ANALYZE 看实际执行耗时。分析时要结合业务频率和返回行数，而不是只看一个字段。
```

## 回答评分点

高分答案应该覆盖：

- `type` 判断访问方式。
- `key` 是实际使用索引。
- `rows` 反映扫描量。
- `Extra` 关注排序、临时表和覆盖索引。
- `EXPLAIN` 要结合业务频率分析。

## 深度完善：面向 L6 的回答框架

围绕「`explain` 重点看哪些字段？」，高分答案不能停在概念定义，而要把「索引、事务、锁、MVCC、分库分表、冷热分层、备份恢复和在线变更」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「数据库和数据建模」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `order、payment、inventory、product 的核心表、唯一约束、索引和归档表` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：慢 SQL、锁等待、死锁数、主从延迟、连接池使用率、归档任务耗时。
- 验证证据：EXPLAIN、慢 SQL 日志、死锁日志、迁移方案、备份恢复演练和容量预测。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：`explain` 重点看哪些字段？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

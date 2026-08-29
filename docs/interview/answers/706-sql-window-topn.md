# 706 用 SQL 窗口函数查询每组 Top N

[返回按分类学习面试题](../README.md)

## 题目

表 `orders(id, user_id, amount, paid_at, status)`，查询每个用户支付金额最高的 3 笔订单；金额相同时按支付时间更晚、订单 ID 更大排序。

这道题不只考会不会写 `ROW_NUMBER()`，还考并列语义、确定性排序、过滤位置和索引成本。

## 标准解法

```sql
WITH ranked AS (
    SELECT id,
           user_id,
           amount,
           paid_at,
           ROW_NUMBER() OVER (
               PARTITION BY user_id
               ORDER BY amount DESC, paid_at DESC, id DESC
           ) AS row_num
    FROM orders
    WHERE status = 'PAID'
)
SELECT id, user_id, amount, paid_at
FROM ranked
WHERE row_num <= 3
ORDER BY user_id, row_num;
```

MySQL 不能在同一查询层的 `WHERE` 中直接引用刚计算的窗口别名，所以需要 CTE 或子查询。最后一个 `id DESC` 提供稳定的 tie-breaker，否则相同金额和时间的两行顺序不确定，分页或回归测试可能抖动。

## 三种排名函数的区别

输入金额 `100, 100, 90`：

| 函数 | 结果 | 适用语义 |
| --- | --- | --- |
| `ROW_NUMBER()` | 1, 2, 3 | 每组严格取 N 行 |
| `RANK()` | 1, 1, 3 | 并列占用后续名次 |
| `DENSE_RANK()` | 1, 1, 2 | 并列不产生名次空洞 |

若产品说“前三名，允许并列”，必须追问可能返回超过三行，以及采用 `RANK` 还是 `DENSE_RANK`。不能擅自替换业务定义。

## 索引与执行成本

候选索引可从过滤和分组排序出发设计，例如：

```sql
CREATE INDEX idx_orders_paid_user_amount
    ON orders(status, user_id, amount DESC, paid_at DESC, id DESC);
```

索引是否减少排序取决于数据分布、优化器计划和版本能力，必须用 `EXPLAIN ANALYZE` 验证。全站每个用户 Top 3 仍需处理全部已支付候选行；索引不能把一个大结果计算变成常数时间。

在线接口通常只查询一个或一批用户：

```sql
SELECT id, user_id, amount, paid_at
FROM orders
WHERE status = 'PAID' AND user_id = ?
ORDER BY amount DESC, paid_at DESC, id DESC
LIMIT 3;
```

全量榜单更适合离线计算、增量物化或按分片并行，避免在交易主库每次现算。

## MyBatis 映射

```java
public interface OrderRankingMapper {
    List<RankedOrder> selectTopPaidOrders(
            @Param("userIds") Collection<Long> userIds,
            @Param("limit") int limit);
}
```

动态 `IN` 参数要限制批量大小；`limit` 在服务端设置上限。集成测试至少覆盖不足 N 行、完全并列、时间并列、非支付订单、多个用户和稳定排序。

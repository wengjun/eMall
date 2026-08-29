# 707 用 SQL 解决连续日期区间问题

[返回按分类学习面试题](../README.md)

## 题目

表 `user_checkin(user_id, checkin_time)` 可能记录用户同一天多次签到。查询每个用户所有连续签到区间的开始日期、结束日期和天数。

这是经典 gaps and islands 问题：先去重为日期，再给连续元素构造相同分组键。

## `ROW_NUMBER` 平移法

连续日期与连续序号同步增加，把日期减去序号后会得到相同锚点：

```text
date        row_num  date - row_num days
2026-08-01  1        2026-07-31
2026-08-02  2        2026-07-31
2026-08-03  3        2026-07-31
2026-08-05  4        2026-08-01
```

MySQL 8 SQL：

```sql
WITH daily AS (
    SELECT DISTINCT user_id, DATE(checkin_time) AS checkin_date
    FROM user_checkin
), numbered AS (
    SELECT user_id,
           checkin_date,
           ROW_NUMBER() OVER (
               PARTITION BY user_id ORDER BY checkin_date
           ) AS row_num
    FROM daily
), grouped AS (
    SELECT user_id,
           checkin_date,
           DATE_SUB(checkin_date, INTERVAL row_num DAY) AS island_key
    FROM numbered
)
SELECT user_id,
       MIN(checkin_date) AS start_date,
       MAX(checkin_date) AS end_date,
       COUNT(*) AS consecutive_days
FROM grouped
GROUP BY user_id, island_key
ORDER BY user_id, start_date;
```

## 用 `LAG` 显式识别断点

另一种思路是比较前一天，遇到间隔大于一天就产生新组标志，再对标志做累计和：

```sql
WITH daily AS (
    SELECT DISTINCT user_id, DATE(checkin_time) AS checkin_date
    FROM user_checkin
), marked AS (
    SELECT user_id,
           checkin_date,
           CASE
               WHEN LAG(checkin_date) OVER (
                       PARTITION BY user_id ORDER BY checkin_date
                    ) = DATE_SUB(checkin_date, INTERVAL 1 DAY)
               THEN 0 ELSE 1
           END AS new_group
    FROM daily
), grouped AS (
    SELECT user_id,
           checkin_date,
           SUM(new_group) OVER (
               PARTITION BY user_id ORDER BY checkin_date
           ) AS group_id
    FROM marked
)
SELECT user_id,
       MIN(checkin_date) AS start_date,
       MAX(checkin_date) AS end_date,
       COUNT(*) AS consecutive_days
FROM grouped
GROUP BY user_id, group_id;
```

`LAG` 方案更容易改成“间隔不超过 7 天算同一活跃区间”等规则，平移法对严格连续整数/日期最简洁。

## 生产边界

- 必须先按用户、业务时区把时间归一为日期；UTC 零点不一定是中国用户的业务日边界。
- 同一天多条记录要去重，否则序号错位。
- `DATE(checkin_time)` 可能让普通时间索引无法直接完成范围过滤；生产可冗余 `checkin_date` 并建 `(user_id, checkin_date)` 唯一索引。
- 全量历史计算放离线任务；在线只计算单用户有限时间窗或读取物化结果。

测试覆盖闰日、跨月/年、时区边界、重复签到、单日区间和多个用户。营销奖励还需数据库唯一发放记录，不能仅凭统计查询防重复发奖。

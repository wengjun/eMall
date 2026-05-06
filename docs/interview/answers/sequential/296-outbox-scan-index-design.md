# 296 如何设计 Outbox 扫描索引？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

如何设计 Outbox 扫描索引？

## 先给面试官的短答案

Outbox 扫描索引要支持投递器快速找到待发送、可重试、到期的消息，并按创建时间或 ID 稳定批量扫描。
常见联合索引是 `(status, next_retry_at, id)` 或 `(status, created_at, id)`，并配合分片字段和批次限制。

索引目标是避免投递器每次扫全表。

## 查询模式

投递器常见查询：

```sql
SELECT *
FROM outbox_event
WHERE status = 'PENDING'
  AND next_retry_at <= now()
ORDER BY id
LIMIT 100;
```

索引要服务这个查询。

## 常见字段

Outbox 表字段：

- `id`。
- `event_id`。
- `status`。
- `next_retry_at`。
- `retry_count`。
- `created_at`。
- `shard_key`。
- `locked_until`。

不同投递模型会影响索引。

## 并发扫描

多投递器并发时要：

- 使用分片字段。
- 使用状态抢占。
- 使用乐观锁。
- 控制批次大小。
- 避免多个实例抢同一批消息。

索引要支持按分片和状态快速定位。

## 在 eMall 项目中怎么讲？

订单服务 Outbox 可以按 `status + next_retry_at + id` 建索引。

投递器每次扫描少量到期未发送事件，抢占成功后发送 MQ，成功后更新为 `SENT`。

## 深度增强：索引设计图

![索引设计从访问路径出发](../../assets/index-design.svg)

Outbox 表的查询路径非常固定：投递器只关心“哪些消息待发送、已经到重试时间、按稳定顺序取一小批”。
所以索引要围绕投递器扫描设计，而不是围绕后台页面随意加索引。

## 深度增强：表结构和索引

```sql
CREATE TABLE outbox_event (
    id BIGINT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    next_retry_at DATETIME NOT NULL,
    retry_count INT NOT NULL,
    locked_until DATETIME NULL,
    created_at DATETIME NOT NULL,
    payload JSON NOT NULL,
    UNIQUE KEY uk_event_id (event_id),
    KEY idx_status_retry_id (status, next_retry_at, id)
);
```

投递器扫描：

```sql
SELECT id, event_id, event_type, payload
FROM outbox_event
WHERE status = 'PENDING'
  AND next_retry_at <= CURRENT_TIMESTAMP
ORDER BY next_retry_at, id
LIMIT 100;
```

如果多实例并发投递，可以增加抢占字段：

```sql
UPDATE outbox_event
SET status = 'SENDING',
    locked_until = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 SECOND)
WHERE id = ?
  AND status = 'PENDING';
```

应用根据影响行数判断是否抢占成功。

## 深度增强：生产边界

- 扫描必须限制批次大小，避免一次拉太多导致长事务或内存膨胀。
- 大表要归档 `SENT` 历史数据，否则索引越来越大。
- 多实例要通过状态抢占、分片或 `skip locked` 避免重复投递。
- `event_id` 要唯一，方便生产排查和消费者幂等。
- 投递失败要更新 `next_retry_at`，支持指数退避和告警。

## 深度增强：面试高分表达

```text
Outbox 索引要服务 relay 的固定查询：按 status 找待发送消息，按 next_retry_at 判断是否到期，
按 id 稳定分页取一小批。因此常用 status + next_retry_at + id。多实例投递时还要做状态抢占，
发送成功后标记 SENT，历史数据定期归档，否则 outbox 会从可靠性组件变成性能瓶颈。
```

## 专家级完整回答

```text
Outbox 索引要围绕投递器查询设计。投递器通常按 status 找待发送消息，再按 next_retry_at 判断是否到期，
按 id 或 created_at 稳定排序批量扫描。

常见索引是 status + next_retry_at + id。如果多实例并发，还要考虑 shard_key、锁定时间和状态抢占，避免全表扫描和重复投递。
```

## 回答评分点

高分答案应该覆盖：

- Outbox 索引服务投递扫描。
- `status`、`next_retry_at`、`id` 常见。
- 要限制批次大小。
- 多实例要考虑分片和抢占。
- 避免全表扫描。
## 深度完善：专项验收清单

围绕「如何设计 Outbox 扫描索引？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
回答时要把概念、代码、数据、失败路径和指标串起来，证明自己不是只理解单点知识。

### 项目落点

- 先说明它在 eMall 哪个模块或链路中出现，例如交易、库存、支付、搜索、风控、发布或可观测性。
- 再说明它保护的核心目标：正确性、可用性、延迟、成本、安全或协作效率。
- 最后补失败场景：超时、重试、重复请求、状态不一致、热点流量、配置错误或发布回滚。

### 验收证据

- 代码证据：关键类、状态机、唯一约束、事务边界、线程池隔离或配置项。
- 测试证据：单元测试、集成测试、契约测试、压测、故障注入或回归用例。
- 运行证据：指标看板、Trace、结构化日志、告警、Runbook、对账结果或补偿记录。

### 高分收束

面试最后要回到取舍：当前方案为什么足够简单可靠，什么时候需要升级，升级时如何灰度、回滚和验证。
这样回答能体现生产系统判断力，而不是只罗列技术名词。

深度完善标记：专题增强答案已补项目落点、验收证据和取舍收束。

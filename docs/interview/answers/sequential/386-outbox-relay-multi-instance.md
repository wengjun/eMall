# 386 Outbox Relay 多实例如何避免重复抢事件？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

Outbox Relay 多实例如何避免重复抢事件？

## 先给面试官的短答案

Outbox Relay 多实例可以通过数据库行级锁、状态机抢占、分片扫描、乐观锁版本号或 `skip locked`
避免多个实例同时处理同一条事件。

即使做了抢占控制，也要允许重复投递，并要求消费者幂等。

## 状态机抢占

Outbox 状态可以包括：

- `NEW`。
- `PROCESSING`。
- `SENT`。
- `FAILED`。

Relay 扫描 `NEW` 事件，通过条件更新抢占：

```sql
UPDATE outbox_event
SET status = 'PROCESSING'
WHERE id = ?
  AND status = 'NEW'
```

更新成功才拥有处理权。

## skip locked

数据库支持时，可以使用：

```sql
SELECT *
FROM outbox_event
WHERE status = 'NEW'
ORDER BY id
LIMIT 100
FOR UPDATE SKIP LOCKED
```

多个 Relay 实例会跳过已被其他事务锁住的行。

## 仍可能重复

重复来源：

- 发送 MQ 成功但标记 SENT 失败。
- Relay 发送超时但 Broker 实际收到。
- 实例宕机在处理中。
- PROCESSING 超时后被其他实例接管。

因此消费者幂等仍然必需。

## 在 eMall 项目中怎么讲？

eMall Outbox Relay 可以按事件 ID 范围或库表分片扫描，并使用状态条件更新抢占事件。

如果同一订单事件因为 Relay 重试被投递两次，库存和履约消费者通过 `event_id` 幂等表保证只处理
一次业务写入。

## 深度增强：Outbox 投递图

![本地事务加 Outbox 的可靠事件发布流程](../../assets/outbox-flow.svg)

多实例 Relay 的目标是提升吞吐和可用性，但它只能降低重复抢占概率，不能从根上消除重复投递。
因此设计重点是“抢占尽量准确，消费必须幂等”。

## 深度增强：状态抢占 SQL

Relay 先扫描候选事件，再用条件更新抢占：

```sql
UPDATE outbox_event
SET status = 'PROCESSING',
    locked_by = #{instanceId},
    locked_until = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 SECOND)
WHERE id = #{eventId}
  AND status = 'NEW';
```

只有影响行数为 1 的实例才拥有投递权：

```java
public final class OutboxClaimService {

    private final OutboxMapper outboxMapper;

    public boolean claim(long eventId, String instanceId) {
        int affectedRows = outboxMapper.claim(eventId, instanceId);
        return affectedRows == 1;
    }
}
```

处理 `PROCESSING` 超时也要谨慎，避免实例还在发送时被别的实例抢走：

```sql
UPDATE outbox_event
SET status = 'NEW',
    locked_by = NULL,
    locked_until = NULL
WHERE status = 'PROCESSING'
  AND locked_until < CURRENT_TIMESTAMP;
```

## 深度增强：生产边界

- 发送成功但标记 `SENT` 失败，会产生重复投递。
- Broker 超时但实际收到消息，也会产生重复投递。
- 分片扫描能减少竞争，但不能替代消费者幂等。
- `PROCESSING` 超时时间要大于正常发送 P99，并结合告警。
- Relay 指标要包括待发送量、最老待发送时间、抢占失败率、重复投递估计值。

## 深度增强：面试高分表达

```text
多实例 Relay 我会用状态机条件更新或 skip locked 抢占事件，只有抢占成功的实例才能发送。
但我仍然认为重复投递不可避免，因为发送成功后标记失败、网络超时和实例宕机都会造成不确定状态。
所以 Outbox 的完整方案必须包含消费者幂等、死信、重试和对账。
```

## 专家级完整回答

```text
Outbox Relay 多实例可以用状态机条件更新、行级锁、skip locked、乐观锁或分片扫描避免重复抢占。
只有成功把 NEW 更新为 PROCESSING 的实例才处理该事件。

但它不能完全避免重复投递，因为发送成功后标记失败、网络超时和实例宕机都会产生不确定状态。
所以消费者幂等是 Outbox 模式的必要组成部分。
```

## 回答评分点

高分答案应该覆盖：

- 状态机抢占。
- 条件更新或行级锁。
- `skip locked` 可用于并发扫描。
- 发送成功标记失败会导致重复。
- 消费者幂等仍然必须。
## 深度完善：专项验收清单

围绕「Outbox Relay 多实例如何避免重复抢事件？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

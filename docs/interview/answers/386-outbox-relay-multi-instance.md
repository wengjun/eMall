# 386 Outbox Relay 多实例如何避免重复抢事件？

[返回按分类学习面试题](../README.md)

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

## 电商系统实践

大型电商系统的 Outbox Relay 可以按事件 ID 范围或库表分片扫描，并使用状态条件更新抢占事件。

如果同一订单事件因为 Relay 重试被投递两次，库存和履约消费者通过 `event_id` 幂等表保证只处理
一次业务写入。

## 深度增强：Outbox 投递图

![本地事务加 Outbox 的可靠事件发布流程](../assets/outbox-flow.svg)

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

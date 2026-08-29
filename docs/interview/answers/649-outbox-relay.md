# 649 手写 Outbox Relay 核心逻辑。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
final class OutboxRelay {
    private final OutboxRepository repository;
    private final MessagePublisher publisher;

    OutboxRelay(OutboxRepository repository, MessagePublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    void publishBatch(int limit) {
        for (OutboxEvent event : repository.lockPending(limit)) {
            try {
                publisher.publish(event.topic(), event.key(), event.payload());
                repository.markPublished(event.id());
            } catch (RuntimeException ex) {
                repository.markRetry(event.id(), ex.getMessage());
            }
        }
    }
}
```

### 必测用例

- 待发送事件被发布并标记成功，发送失败时保留可重试状态和下次执行时间。
- 多 Relay 实例不能同时长期占有同一事件。
- 重点模拟消息已发送但成功标记前崩溃，确认会重复投递且消费者能够幂等。

### 生产化差异

- 需要分片或租约抢占、指数退避、毒消息隔离、积压告警和历史清理。
- Outbox 提供至少一次投递，不承诺业务恰好一次。
- 数据库事务不能覆盖 MQ 发送；发送成功但状态更新失败时必须允许重复，并由消费者幂等收敛。

# 650 手写 MQ 消费端去重逻辑。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```sql
create table consumed_message (
    message_id varchar(128) primary key,
    consumer_group varchar(128) not null,
    consumed_at timestamp not null
);
```

```java
boolean firstConsume = consumedMessageRepository.tryInsert(messageId, consumerGroup);
if (!firstConsume) {
    return;
}
handleBusinessMessage(message);
```

### 必测用例

- 同一消息重复和并发到达时业务副作用只发生一次。
- 业务处理失败时去重记录不能被错误提交，否则后续重试会被吞掉。
- 模拟提交业务事务后确认消息前崩溃，验证重投仍返回成功。

### 生产化差异

- 去重记录与业务写入应处于同一数据库事务，并使用消息或业务唯一键。
- 设计保留期、分区和冲突指标；天然幂等的条件更新可以避免通用去重表。

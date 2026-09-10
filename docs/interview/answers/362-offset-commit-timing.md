# 362 Spring Kafka 如何决定 offset 提交时机？

[返回按分类学习面试题](../README.md)

## 区分 Kafka 客户端与 Spring 容器

原生 KafkaConsumer 的 enable.auto.commit 和 Spring 容器的 AckMode 是两套配置。
使用同步 record listener 时，常见选择是关闭原生自动提交，让容器在监听器成功返回后提交。

```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false
    listener:
      ack-mode: record
```

```java
@KafkaListener(topics = "orders", groupId = "order-projection")
public void consume(String payload) {
    projectionService.apply(payload);
}
```

这是监听 Bean 的片段，projectionService 是另一个 Bean；其事务方法完成提交后才返回。
不要在监听器里提交异步任务后立即返回，也不要吞掉异常，否则容器可能把尚未成功处理的记录视为完成。

## AckMode 的具体区别

RECORD 按记录完成提交；BATCH 等一轮 poll 返回的记录处理完成后提交。
MANUAL 的 acknowledge 不代表总是当场提交；MANUAL_IMMEDIATE 的即时行为还取决于是否在消费线程调用。
不是所有生产消费都需要自己手写 commitSync。语义见
[Spring Kafka 提交模式](https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/message-listener-container.html)。

原生客户端提交的位置是“下一条要消费的 offset”，通常为最后连续处理成功的 offset + 1。
KafkaConsumer 不能随意跨线程共享，不能让多个工作线程独立推进同一分区的提交位置。
数据库成功而 offset 尚未提交仍可能重复消费，这一点不会因选择某个 AckMode 自动消失。

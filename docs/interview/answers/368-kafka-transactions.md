# 368 Kafka Java 事务 API 如何使用？

[返回按分类学习面试题](../README.md)

## 学调用顺序，不重复分布式事务设计

KafkaProducer 配置非空 transactional.id 后，先 initTransactions，再开始事务、发送、提交或中止。
并行活跃的生产者不能共用同一个 transactional.id，否则会触发 fencing。

```java
producer.initTransactions();
producer.beginTransaction();
producer.send(new ProducerRecord<>("order-events", orderId, event));
producer.send(new ProducerRecord<>("order-audit", orderId, audit));
producer.commitTransaction();
```

这是成功路径片段，不是完整消费循环。commitTransaction 会处理该事务的待发送记录。
接收方需要 isolation.level=read_committed，才能按已提交事务读取。
失败路径不能一律“重试整个代码块”：ProducerFencedException 等致命错误要求关闭该 Producer；
可中止的错误按客户端契约 abortTransaction，提交超时等情况也要按 API 的状态约束处理。

## 消费后再生产

输入 offset 不会因 beginTransaction 自动加入事务。需要把已连续处理成功的位置通过
sendOffsetsToTransaction(offsets, consumer.groupMetadata()) 纳入，再提交 Kafka 事务。
位置仍为下一条要读的 offset，且不能同时让消费者自动提交这些位置。

Spring 的事务监听容器与 KafkaTransactionManager 可以管理这段流程；
KafkaTemplate.executeInTransaction 适合显式生产事务，不等于已把任何数据库事务也包进来。
API 边界见 [KafkaProducer](https://kafka.apache.org/37/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html)。

学习时验证两个 Java 客户端行为即可：中止事务的输出对 read_committed 不可见；
事务失败后输入位置不会被当作成功处理推进。数据库、HTTP 副作用不在这个原子范围内。

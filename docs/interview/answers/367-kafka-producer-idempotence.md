# 367 Kafka Java Producer 的幂等如何配置？

[返回按分类学习面试题](../README.md)

## 只看客户端配置与生命周期

幂等 Producer 解决客户端协议重试中的重复追加，不识别应用再次调用 send 的业务重复。
下面是 Kafka 3.x Producer 的属性片段，bootstrap.servers 和序列化器仍需正常配置：

```java
properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
properties.put(ProducerConfig.ACKS_CONFIG, "all");
properties.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);
```

这些配置有相互约束，不能同时开启幂等又把 acks 改成 0。
retries 很大不等于无限重试，delivery.timeout.ms 仍限制发送交付时间。
应满足 delivery.timeout.ms 不小于 request.timeout.ms 与 linger.ms 的组合要求。
约束见 [Kafka Producer API](https://kafka.apache.org/37/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html)。

## Java 使用的两个坑

KafkaProducer 可由多线程复用，不要每条消息新建；关闭时要等待或明确处理待发送记录。
send 返回 Future，只说明交给客户端处理，不表示 Broker 已确认。

```java
producer.send(new ProducerRecord<>("orders", orderId, payload), (metadata, exception) -> {
    if (exception != null) {
        failureReporter.record(orderId, exception);
    }
});
```

这是异步发送片段，failureReporter 必须快速返回，不能在 Producer 回调线程里阻塞查库。
立即发生的序列化、元数据等待等异常还可能直接从 send 抛出，也需要调用端处理。
手工重新 send 是一条新发送请求，不能靠协议幂等推断它一定会被去重。

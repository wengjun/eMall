# 383 Spring Kafka 如何处理反序列化失败？

[返回按分类学习面试题](../README.md)

## 为什么监听方法里的 catch 不够

Deserializer 在 KafkaConsumer.poll 过程中运行，失败时消息可能还没进入 @KafkaListener。
因此需要在反序列化器外层使用 ErrorHandlingDeserializer，把错误交给 Spring 容器处理。

下面是 ConsumerFactory 的构造片段，OrderEvent 为明确的事件类型，properties 包含连接及消费组属性：

```java
var delegate = new JsonDeserializer<>(OrderEvent.class, false);
var valueDeserializer = new ErrorHandlingDeserializer<>(delegate);

var consumerFactory = new DefaultKafkaConsumerFactory<>(
        properties, new StringDeserializer(), valueDeserializer);
```

使用 Spring Kafka 的 JsonDeserializer、ErrorHandlingDeserializer，以及 Kafka 的 StringDeserializer。
这里不依赖外部 header 任意选择 Java 类型，也不要为了省事将 trusted packages 设置成 *。

## 错误处理器的装配点

非事务容器可在监听工厂上配置 DefaultErrorHandler，并用 DeadLetterPublishingRecoverer 隔离坏消息。
ErrorHandlingDeserializer 保留错误信息及原始字节，recoverer 要保存 topic、partition、offset 等定位信息。

需要特别检查死信 Producer 的序列化器：反序列化失败的原始内容可能是 byte[]，
不能一律再按正常业务对象 JSON 序列化。应按类型选择合适的 serializer。
recoverer 的发送失败必须向上报告，不能在死信尚未可靠保存时把原消息视为恢复成功。
相关扩展点见 [Spring Kafka 异常处理](https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html)。

事务容器还涉及回滚和 AfterRollbackProcessor，不能照搬非事务处理器的提交假设。
测试时直接生产非法字节，而不是只让业务方法主动抛一个“模拟反序列化异常”。

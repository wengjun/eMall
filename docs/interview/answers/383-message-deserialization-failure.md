# 383 如何处理消息反序列化失败？

[返回按分类学习面试题](../README.md)

## 题目

如何处理消息反序列化失败？

## 先给面试官的短答案

消息反序列化失败通常说明 schema 不兼容、消息损坏、字段类型变化或生产者发送了非法数据。
处理时不能让消费者无限重试阻塞主链路，应记录原始消息、错误原因和位置，然后投递死信或隔离队列。

同时要告警并推动 schema 修复。

## 常见原因

原因：

- 字段类型不兼容。
- 必填字段缺失。
- 消息不是预期格式。
- 生产者版本错误。
- 历史消息无法被新代码解析。
- 编码或压缩配置错误。

反序列化失败通常不可通过立即重试解决。

## 处理方式

方式：

- 捕获反序列化异常。
- 记录 Topic、Partition、offset 和 key。
- 保存原始 payload。
- 发送到死信 Topic。
- 提交或跳过该消息前要确保已隔离。
- 触发告警。

不要让毒消息反复阻塞消费。

## 修复流程

修复：

- 确认生产者版本。
- 对比 schema 变更。
- 修复消费者兼容性。
- 回放死信消息。
- 增加契约测试。
- 完善发布流程。

死信不是终点，要能修复和回放。

## 在 eMall 项目中怎么讲？

eMall 订单事件反序列化失败时，消费者应把原始消息写入 `order-events-dlq`，并记录 offset 和
trace ID。

修复 schema 后，通过运维工具从死信 Topic 回放，不能直接丢弃核心订单事件。

## 深度增强：缓存和消息治理图

![数据库、缓存和消息一致性链路](../assets/data-cache-mq.svg)

缓存和消息题要关注一致性、削峰、延迟、积压和恢复。
Redis 很快，但会遇到穿透、击穿、雪崩、热点 key 和内存淘汰；
MQ 能解耦和削峰，但会带来重复消费、乱序、积压和死信处理。

## 深度增强：Java 17 幂等消费示例

```java
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class LocalIdempotentConsumer {
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    boolean tryHandle(String messageKey, Runnable handler) {
        if (!processedKeys.add(messageKey)) {
            return false;
        }
        handler.run();
        return true;
    }
}
```

这个示例只适合解释幂等思想。生产环境不能用本地内存做全局幂等，要使用数据库唯一键、Redis 原子操作或业务状态机。

## 深度增强：生产边界

缓存要有 TTL、容量、降级和回源保护；消息要有重试、死信、延迟队列、消费幂等和积压告警。
缓存不一致要能修复，消息失败要能回放，不能只依赖人工查日志。

## 深度增强：面试高分表达

我会把缓存和消息都看成性能与稳定性工具，而不是正确性事实来源。
正确性由数据库事实、状态机、幂等和对账保证；缓存和 MQ 负责降低延迟、削峰填谷和解耦系统。

## 专家级完整回答

```text
反序列化失败多半是 schema 或数据格式问题，立即重试通常没有意义。消费者应隔离该消息，保存原始
payload、Topic、Partition、offset、错误原因和 trace ID，投递死信并告警。

后续通过修复 schema、发布兼容消费者和回放死信来恢复。核心事件不能静默丢弃。
```

## 回答评分点

高分答案应该覆盖：

- 反序列化失败通常不是瞬时故障。
- 保存原始消息和 offset。
- 投递死信或隔离。
- 告警和修复 schema。
- 核心消息要支持回放。

# 217 本地事务加 Outbox 解决什么问题？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

本地事务加 Outbox 解决什么问题？

## 先给面试官的短答案

本地事务加 Outbox 解决“业务数据写成功，但消息发送失败”导致的数据和消息不一致问题。
做法是在同一个数据库本地事务里写业务表和消息表，再由后台任务或 CDC 把消息可靠投递到 MQ。

它把跨系统原子性问题转换为本地事务加异步可靠投递问题。

## 基本流程

流程：

- 开启本地事务。
- 写业务表。
- 写 outbox 消息表。
- 提交本地事务。
- 投递器扫描或 CDC 捕获消息。
- 发送到 MQ。
- 成功后标记已发送。

业务数据和待发送消息在一个本地事务中提交。

## 解决的问题

解决：

- 业务提交成功但 MQ 发送失败。
- 应用发送 MQ 后宕机。
- MQ 短时不可用。
- 需要可靠发布领域事件。

只要业务事务提交，消息最终能被投递。

## 工程要点

要点包括：

- outbox 表有状态和重试次数。
- 投递器幂等。
- 消息有唯一 ID。
- 消费者幂等。
- 失败进入告警或人工处理。
- 表数据定期归档。

Outbox 只保证消息最终发出，不保证消费者一定处理成功。

## 在 eMall 项目中怎么讲？

订单服务创建订单时，在同一个事务中写订单表和 `OrderCreated` 事件到 outbox 表。

事务提交后，事件投递器把 `OrderCreated` 发送到 MQ，库存、营销、数据仓库等服务再消费。

## 深度增强：流程图

![本地事务加 Outbox 的可靠事件发布流程](../assets/outbox-flow.svg)

这张图要抓住一个核心点：业务表和 outbox 表必须在同一个数据库本地事务中提交。
只要这个事务提交成功，消息即使暂时没有发到 MQ，也已经以数据形式持久化下来，后续可以重试。

## 深度增强：Java 17 代码实现

下面代码不是完整框架代码，而是面试中最应该讲清楚的核心逻辑：业务写入和事件写入同事务提交。

```java
public record OutboxEvent(
        String eventId,
        String aggregateId,
        String eventType,
        String payload,
        OutboxStatus status,
        int retryCount) {
}

public enum OutboxStatus {
    NEW,
    SENT,
    FAILED
}

@Service
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderApplicationService(
            OrderRepository orderRepository,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderId createOrder(CreateOrderCommand command) {
        Order order = Order.create(command.userId(), command.items());
        orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                order.id().value(),
                command.userId().value(),
                order.totalAmount());

        outboxRepository.save(new OutboxEvent(
                event.eventId(),
                order.id().value(),
                "OrderCreated",
                toJson(event),
                OutboxStatus.NEW,
                0));

        return order.id();
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox event.", ex);
        }
    }
}
```

投递器的重点不是“扫描一遍发送 MQ”这么简单，而是要支持抢占、重试、幂等和失败可观测。

```java
public final class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final MessagePublisher publisher;

    public OutboxRelay(OutboxRepository outboxRepository, MessagePublisher publisher) {
        this.outboxRepository = outboxRepository;
        this.publisher = publisher;
    }

    public void publishBatch(int batchSize) {
        List<OutboxEvent> events = outboxRepository.lockNextBatch(batchSize);
        for (OutboxEvent event : events) {
            try {
                publisher.publish(event.eventType(), event.eventId(), event.payload());
                outboxRepository.markSent(event.eventId());
            } catch (RuntimeException ex) {
                outboxRepository.markFailedForRetry(event.eventId(), ex.getMessage());
            }
        }
    }
}
```

面试里要补一句：真实生产中 `lockNextBatch` 需要防止多个 relay 实例重复抢同一批数据。
MySQL 可以用状态条件更新、版本号、`select for update skip locked` 或分片扫描来实现。

## 深度增强：失败场景和边界

Outbox 能解决“业务提交成功但消息没发出去”，但不能解决所有一致性问题。

必须继续补齐：

- 投递重复：MQ 或 relay 重试可能导致重复消息，消费者必须幂等。
- 投递乱序：同一聚合根最好按 `aggregateId` 选择同一个 partition。
- 表无限增长：outbox 需要按状态和创建时间归档。
- 毒消息：反序列化失败或业务字段异常时要进入死信和人工处理。
- 端到端失败：Outbox 只保证事件最终发布，不保证消费者一定处理成功。

## 深度增强：面试高分表达

回答时不要只说“写 outbox 表”。更好的表达是：

```text
我把跨系统原子性拆成两段：第一段用本地事务保证业务状态和待发布事件同时提交；
第二段用可重试 relay 保证事件最终投递。因为投递和消费都可能重复，所以事件要有全局唯一 eventId，
消费者要用幂等表或业务唯一键去重。最后用 outbox 积压量、最老未发送时间、失败重试次数和死信数量做监控。
```

## 专家级完整回答

```text
本地事务加 Outbox 解决业务数据和消息发送之间的原子性问题。业务表和 outbox 消息表在同一个数据库事务中提交，
提交后由投递器或 CDC 把消息发送到 MQ。

这样只要业务数据写成功，消息就不会丢。后续仍需要投递重试、消费者幂等、死信处理和 outbox 表归档。
```

## 回答评分点

高分答案应该覆盖：

- Outbox 解决业务数据和消息原子性。
- 业务表和消息表在同一事务提交。
- 后台投递或 CDC 发送 MQ。
- 投递和消费都要幂等。
- Outbox 不等于端到端处理成功。
## 深度完善：专项验收清单

围绕「本地事务加 Outbox 解决什么问题？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

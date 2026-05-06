# 381 消费端如何保证业务写入和去重记录原子性？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

消费端如何保证业务写入和去重记录原子性？

## 先给面试官的短答案

消费端要把去重记录和业务写入放在同一个本地数据库事务里。事务内先插入幂等记录，成功后执行业务
写入，最后提交事务。事务提交成功后再提交 Kafka offset。

这样重复消息会被唯一约束挡住，业务不会重复执行。

## 为什么要同事务

如果不同事务：

- 去重记录成功，业务写入失败，会误认为已处理。
- 业务写入成功，去重记录失败，重复消息会再次执行业务。
- 宕机点难以恢复。

所以去重和业务写入必须同生共死。

## 推荐流程

流程：

- 开启数据库事务。
- 插入幂等表，唯一键为消费组加事件 ID。
- 唯一冲突则判断已处理。
- 执行业务写入。
- 更新幂等表状态为成功。
- 提交数据库事务。
- 手动提交 offset。

offset 不应该早于本地事务提交。

## 异常处理

异常场景：

- 唯一冲突且状态成功，直接跳过。
- 唯一冲突且状态处理中，判断是否超时。
- 业务失败，回滚事务。
- 多次失败后进入重试或死信。

处理中状态要有超时恢复机制。

## 在 eMall 项目中怎么讲？

eMall 库存消费者收到订单创建事件后，在同一个事务中插入幂等记录并扣减库存。

如果消费者在提交 offset 前宕机，消息会重放，但再次插入幂等记录会命中唯一约束，不会重复扣减。

## 深度增强：事务边界图

![库存防超卖和消费幂等的事务边界](../../assets/inventory-idempotency.svg)

这道题的关键不是 Kafka，而是本地事务边界。Kafka offset、幂等表、业务表三者不能在一个全局事务里，
所以要保证本地数据库先正确提交，再提交 offset。

## 深度增强：Java 17 事务代码

```java
@Service
public class ReliableOrderEventConsumer {

    private final DeduplicationRepository deduplicationRepository;
    private final FulfillmentRepository fulfillmentRepository;

    public ReliableOrderEventConsumer(
            DeduplicationRepository deduplicationRepository,
            FulfillmentRepository fulfillmentRepository) {
        this.deduplicationRepository = deduplicationRepository;
        this.fulfillmentRepository = fulfillmentRepository;
    }

    @Transactional
    public ConsumeResult consume(OrderPaidEvent event) {
        boolean inserted = deduplicationRepository.insertProcessing(
                "fulfillment-order-paid",
                event.eventId(),
                event.orderId());

        if (!inserted) {
            return ConsumeResult.duplicate(event.eventId());
        }

        fulfillmentRepository.createShipmentPlan(event.orderId(), event.items());
        deduplicationRepository.markSuccess("fulfillment-order-paid", event.eventId());
        return ConsumeResult.success(event.eventId());
    }
}
```

Kafka listener 应在 `consume` 成功返回后再手动提交 offset。不要在收到消息后立刻 ack。

## 深度增强：失败场景

- 本地事务提交成功，offset 提交失败：消息会重放，幂等表挡住重复业务。
- 本地事务回滚，offset 未提交：消息会重试，业务可以再次执行。
- 幂等记录为 `PROCESSING` 后宕机：需要超时恢复或人工处理。
- 同一事件被不同消费组消费：幂等唯一键必须包含 `consumer_group`。

## 深度增强：面试高分表达

```text
我不会试图让 Kafka offset 和数据库业务写入做分布式事务。正确做法是把幂等记录和业务写入放在同一个本地事务里，
事务提交后再提交 offset。本地事务成功但 offset 失败时，消息会重复投递，但唯一键会阻止重复执行业务。
```

## 专家级完整回答

```text
消费端原子性要靠本地事务。把幂等表插入和业务写入放在同一个数据库事务中，事务提交后再提交
Kafka offset。这样要么都成功，要么都失败。

重复消费时，consumer_group + event_id 的唯一约束会挡住重复业务执行。对于 PROCESSING 状态，
还要设计超时恢复、重试和死信。
```

## 回答评分点

高分答案应该覆盖：

- 去重记录和业务写入同事务。
- 唯一约束防重复。
- offset 在事务成功后提交。
- 处理中状态要能恢复。
- 结合库存或履约说明。
## 深度完善：专项验收清单

围绕「消费端如何保证业务写入和去重记录原子性？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

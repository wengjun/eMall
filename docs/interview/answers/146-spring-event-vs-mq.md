# 146 Spring 事件和 MQ 事件有什么区别？

[返回按分类学习面试题](../README.md)

## 题目

Spring 事件和 MQ 事件有什么区别？

## 先给面试官的短答案

Spring 事件是进程内事件，通常用于同一应用内部模块解耦；MQ 事件是进程间事件，用于跨服务异步通信、削峰和最终一致。
Spring 事件不提供跨实例可靠投递，应用重启后事件可能丢失；MQ 通常提供持久化、重试、消费组和堆积能力。

不要用 Spring 事件替代跨服务消息。

## Spring 事件

Spring 事件特点：

- 同 JVM 内。
- 使用简单。
- 适合模块内解耦。
- 默认不保证跨进程可靠。
- 可以同步或异步执行。

适合本服务内部通知，例如刷新本地缓存、触发本地审计。

## MQ 事件

MQ 事件特点：

- 跨服务。
- 可持久化。
- 支持重试。
- 支持消费组。
- 支持削峰。
- 支持异步解耦。

适合订单创建后通知库存、履约、积分、营销等服务。

## 可靠性差异

Spring 事件发布后，如果应用崩溃，事件可能丢失。

MQ 事件通常写入 broker，可在消费者失败后重试。

但 MQ 也不是绝对一次，消费者必须幂等。

## 事务边界

Spring 事件如果在事务提交前发布，监听器可能看到未提交数据。

可以使用事务事件监听：

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
```

跨服务事件更推荐 Outbox + MQ。

## 在 eMall 项目中怎么讲？

订单服务内部可以用 Spring 事件触发本地审计或缓存清理。

订单创建后通知履约、积分、推荐，应该使用 MQ 事件。

如果订单创建和消息发送要一致，使用 Outbox 模式。

## 深度增强：Spring 服务治理图

![Spring 微服务调用栈和治理边界](../assets/spring-service-stack.svg)

Spring 题要从框架机制讲到业务边界。Controller 负责协议适配，Service 负责业务事务，Repository 负责数据访问；
事务、AOP、校验、错误码、配置和观测都是为了让微服务在复杂调用中保持稳定。

## 深度增强：Java 17 分层示例

```java
record CreateOrderCommand(long userId, long skuId, int quantity) {
}

record CreateOrderResult(long orderId, String status) {
}

interface OrderApplicationService {
    CreateOrderResult create(CreateOrderCommand command);
}

final class OrderControllerAdapter {
    private final OrderApplicationService service;

    OrderControllerAdapter(OrderApplicationService service) {
        this.service = service;
    }

    CreateOrderResult submit(CreateOrderCommand command) {
        return service.create(command);
    }
}
```

这个示例表达分层边界：接口层不堆业务逻辑，业务层不依赖 Web 协议，命令和结果对象形成稳定契约。

## 深度增强：生产边界

框架默认值不能替代设计。事务传播、异常回滚、异步线程池、连接池、序列化、超时和重试都要显式治理。
尤其在订单、支付、库存链路中，要避免长事务、隐式重试和跨服务事务误用。

## 深度增强：面试高分表达

我会先解释框架原理，再说明在电商系统里怎么落地。高分回答要能把自动配置、AOP、事务、MVC、WebFlux、
校验和错误处理，连接到可维护性、可观测性、稳定性和故障恢复。

## 专家级完整回答

```text
Spring 事件是进程内事件，适合同一服务内部模块解耦；MQ 事件是跨进程事件，适合跨服务异步通信、
削峰和最终一致。Spring 事件不提供跨实例可靠投递，MQ 有持久化和重试，但消费者要幂等。

事务后事件要关注提交时机，跨服务可靠事件我会用 Outbox + MQ，而不是直接在事务里发消息。
```

## 回答评分点

高分答案应该覆盖：

- Spring 事件是进程内。
- MQ 是跨服务。
- 可靠性和持久化不同。
- 事务提交时机。
- Outbox + MQ。

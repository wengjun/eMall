# 146 Spring 事件和 MQ 事件有什么区别？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

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

![Spring 微服务调用栈和治理边界](../../assets/spring-service-stack.svg)

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

## 深度完善：面向 L6 的回答框架

围绕「Spring 事件和 MQ 事件有什么区别？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「Spring Boot 和服务工程」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `各服务的 Controller、ApplicationService、MyBatis Plus Mapper、Actuator 和 RestClient` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：事务失败率、健康检查状态、依赖调用耗时、配置变更次数、启动耗时。
- 验证证据：Spring Boot 测试、集成测试、配置审计、Actuator 指标和链路 Trace。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：Spring 事件和 MQ 事件有什么区别？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

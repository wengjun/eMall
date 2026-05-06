# 119 为什么推荐构造函数注入？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

为什么推荐构造函数注入？

## 先给面试官的短答案

构造函数注入能让依赖显式、支持 `final`、保证对象创建后处于完整可用状态，方便单元测试，并能更早暴露循环依赖和职责过重问题。
它符合不可变对象和清晰依赖边界的工程原则。

## 依赖显式

构造函数参数就是类的依赖清单。

阅读类时不用搜索字段上的注解，就能知道它需要什么。

这对大型项目很重要。

## 支持 final

构造函数注入可以这样写：

```java
private final OrderRepository repository;

public OrderService(OrderRepository repository) {
    this.repository = repository;
}
```

依赖创建后不再变化，线程安全和可理解性更好。

## 对象状态完整

对象一旦构造成功，就具备所有必需依赖。

不会出现对象已经创建，但某个字段还没注入导致 NPE 的问题。

这符合“构造完成即有效”的设计原则。

## 单元测试方便

测试中可以直接 new 对象：

```java
OrderService service = new OrderService(fakeRepository, fakePaymentClient);
```

不需要启动 Spring 容器，也不需要反射设置私有字段。

这能提高测试速度和可维护性。

## 暴露循环依赖

构造函数注入会更早暴露循环依赖。

循环依赖通常说明模块边界或职责设计有问题。

字段注入可能让问题隐藏更久。

## 暴露职责过重

如果构造函数有很多参数，说明这个类可能承担太多职责。

这是一种设计反馈。

不要为了让代码看起来短就改用字段注入。

## 在 eMall 项目中怎么讲？

订单服务如果依赖库存、支付、风控、营销、物流、通知、报表等十几个组件，构造函数会很长。

这不是构造函数注入的问题，而是订单服务职责可能需要拆成应用服务、领域服务、协调器和事件处理器。

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
推荐构造函数注入是因为它让必需依赖显式，支持 final，保证对象构造完成后就是完整有效状态，
并且单元测试可以直接 new 对象，不依赖 Spring 容器。它还能更早暴露循环依赖和职责过重。

如果构造函数很长，我会把它当成设计问题处理，而不是用字段注入隐藏依赖复杂度。
```

## 回答评分点

高分答案应该覆盖：

- 依赖显式。
- 支持 final。
- 对象构造后完整。
- 测试方便。
- 暴露循环依赖和职责过重。

## 深度完善：面向 L6 的回答框架

围绕「为什么推荐构造函数注入？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
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

本题复习重点：为什么推荐构造函数注入？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。


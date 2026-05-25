# 119 为什么推荐构造函数注入？

[返回按分类学习面试题](../README.md)

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

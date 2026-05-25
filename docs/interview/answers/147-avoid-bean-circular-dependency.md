# 147 如何避免 Bean 循环依赖？

[返回按分类学习面试题](../README.md)

## 题目

如何避免 Bean 循环依赖？

## 先给面试官的短答案

避免 Bean 循环依赖的关键是拆清职责和依赖方向。优先使用构造函数注入让循环依赖尽早暴露，
再通过提取领域服务、应用服务、事件、接口倒置或中介协调器打破双向依赖。不要依赖字段注入和懒加载掩盖设计问题。

循环依赖通常是架构边界不清晰的信号。

## 循环依赖是什么？

示例：

```text
OrderService -> PaymentService
PaymentService -> OrderService
```

两个 Bean 相互依赖，容器创建时会陷入环。

字段注入可能在某些情况下绕过去，但设计问题仍然存在。

## 为什么推荐构造函数注入？

构造函数注入会让循环依赖启动时暴露。

这比运行中某个路径 NPE 或事务不生效更好。

暴露问题是好事，说明边界需要调整。

## 打破方式一：重新划分职责

如果两个服务互相调用，可能说明职责混在一起。

可以拆出：

- OrderApplicationService。
- PaymentApplicationService。
- OrderDomainService。
- PaymentCallbackHandler。

让依赖单向。

## 打破方式二：事件解耦

如果 A 完成后通知 B，不一定要同步调用。

可以发布事件：

```text
OrderPaidEvent -> listener updates order projection
```

事件能降低直接依赖。

## 打破方式三：接口倒置

高层模块依赖抽象接口，而不是直接依赖具体实现。

但接口倒置不是让循环换个名字，依赖方向仍要清楚。

## 不推荐方式

不推荐：

- 滥用 `@Lazy`。
- 改成字段注入。
- 从 ApplicationContext 手动 getBean。
- 把所有逻辑塞到一个大 Service。

这些通常是在隐藏问题。

## 在 eMall 项目中怎么讲？

订单和支付不应互相直接调用。

支付回调可以调用订单应用服务推进订单状态，订单创建可以发布支付待处理事件。

依赖方向要围绕业务流程和状态机设计，而不是两个 Service 随意互调。

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
Bean 循环依赖通常说明职责边界或依赖方向有问题。我会优先用构造函数注入暴露它，然后通过拆分应用服务和领域服务、
事件解耦、接口倒置或流程协调器打破双向依赖。

@Lazy、字段注入和手动 getBean 只能作为临时手段，不应该作为架构方案。
```

## 回答评分点

高分答案应该覆盖：

- 循环依赖是设计信号。
- 构造函数注入暴露问题。
- 拆职责和单向依赖。
- 事件可以解耦。
- 不滥用 `@Lazy`。

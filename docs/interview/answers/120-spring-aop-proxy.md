# 120 Spring AOP 的代理机制是什么？

[返回按分类学习面试题](../README.md)

## 题目

Spring AOP 的代理机制是什么？

## 先给面试官的短答案

Spring AOP 主要通过代理实现，在目标 Bean 外面包一层代理对象，方法调用先进入代理，再执行切面逻辑和目标方法。
如果目标类实现了接口，默认可使用 JDK 动态代理；如果没有接口或配置强制使用类代理，会使用 CGLIB 创建子类代理。

事务、日志、监控、权限等横切逻辑常通过 AOP 实现。

## 代理做了什么？

代理对象拦截方法调用。

流程：

```text
caller -> proxy -> advice -> target method -> advice -> return
```

调用方以为调用的是原始 Bean，实际调用的是代理 Bean。

## JDK 动态代理

JDK 动态代理基于接口。

要求目标类实现接口。

代理对象实现同样接口，调用时通过 `InvocationHandler` 拦截。

优点是不需要生成目标类子类。

缺点是只能代理接口方法。

## CGLIB 代理

CGLIB 通过生成目标类的子类来代理。

它可以代理没有接口的类。

限制：

- final 类不能代理。
- final 方法不能增强。
- private 方法不能通过子类重写增强。

Spring Boot 中很多场景会使用 CGLIB。

## AOP 常见用途

常见横切逻辑：

- 事务。
- 日志。
- 权限。
- 指标。
- 链路追踪。
- 重试。
- 缓存。

它能避免这些逻辑散落在业务代码中。

## 自调用问题

同一个类内部方法调用不会经过代理。

示例：

```java
public void outer() {
    inner();
}

@Transactional
public void inner() {
}
```

如果 `outer` 和 `inner` 在同一个类中，`outer` 内部调用 `inner` 可能绕过事务代理。

这是 Spring AOP 面试高频问题。

## 代理创建时机

AOP 代理通常由 BeanPostProcessor 在 Bean 初始化后创建。

因此容器中最终暴露给其他 Bean 的可能是代理对象，而不是原始对象。

## 在 eMall 项目中怎么讲？

eMall 的订单创建方法可以通过 AOP 做事务、指标和审计。

但事务边界必须清楚：事务方法不能通过同类自调用触发，也不要在事务里调用远程支付或库存服务。

否则事务代理虽然生效，业务设计仍然可能有长事务风险。

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
Spring AOP 基于代理。容器给目标 Bean 创建代理对象，外部调用先进入代理，再执行切面逻辑和目标方法。
实现上有 JDK 动态代理和 CGLIB：JDK 代理基于接口，CGLIB 通过子类增强类。final 类、final 方法和 private
方法不适合 CGLIB 增强。

要特别注意自调用绕过代理，这会导致 @Transactional 等切面不生效。AOP 适合事务、日志、监控等横切逻辑，
但不能替代清晰的业务边界设计。
```

## 回答评分点

高分答案应该覆盖：

- Spring AOP 基于代理。
- JDK 动态代理和 CGLIB。
- 代理对象包裹目标对象。
- 自调用会绕过代理。
- AOP 常用于事务、日志、监控。

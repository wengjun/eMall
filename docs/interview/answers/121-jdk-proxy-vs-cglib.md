# 121 JDK 动态代理和 CGLIB 有什么区别？

[返回按分类学习面试题](../README.md)

## 题目

JDK 动态代理和 CGLIB 有什么区别？

## 先给面试官的短答案

JDK 动态代理基于接口生成代理对象，要求目标类实现接口；CGLIB 通过生成目标类子类实现代理，
不要求接口。JDK 代理拦截接口方法，CGLIB 代理通过方法重写增强目标类方法，因此 final 类、
final 方法和 private 方法不能被 CGLIB 正常增强。

Spring AOP 会根据目标类、接口和配置选择代理方式。

## JDK 动态代理

JDK 动态代理使用 Java 标准库。

核心特点：

- 基于接口。
- 代理类实现目标接口。
- 通过 `InvocationHandler` 拦截调用。
- 只能代理接口方法。

示意：

```text
client -> proxy implements Interface -> InvocationHandler -> target
```

如果目标类没有接口，JDK 动态代理不适用。

## CGLIB

CGLIB 通过字节码生成目标类子类。

核心特点：

- 基于继承。
- 不要求目标类实现接口。
- 通过重写方法实现增强。
- final 类不能代理。
- final 方法不能增强。
- private 方法不能通过子类重写增强。

示意：

```text
client -> proxy extends TargetClass -> interceptor -> target method
```

## Spring 如何选择？

常见规则：

- 目标类有接口时，可以使用 JDK 动态代理。
- 没有接口时，使用 CGLIB。
- 配置强制类代理时，使用 CGLIB。

Spring Boot 中很多场景默认倾向使用 CGLIB，但面试回答要理解两种机制差异。

## 性能不是主要选择标准

早期经常比较两者性能。

现代 JVM 和框架下，选择重点通常不是微小性能差异，而是：

- 是否有接口。
- 是否需要代理类方法。
- 是否存在 final 限制。
- 团队设计规范。
- AOP 是否能正确生效。

## 常见坑

常见问题：

- 自调用绕过代理。
- final 方法事务不生效。
- private 方法无法增强。
- 注入接口和注入实现类时代理类型不同。
- 代理对象不是目标对象本身。

这些都和代理机制有关。

## 在 eMall 项目中怎么讲？

eMall 的 Service 层如果通过接口暴露能力，可以使用 JDK 动态代理。

如果没有接口，Spring 可能使用 CGLIB。

事务、审计、指标等 AOP 能否生效，要看调用是否经过代理，以及目标方法是否可以被代理增强。

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
JDK 动态代理基于接口，代理对象实现目标接口，并通过 InvocationHandler 拦截接口方法。
CGLIB 基于继承，生成目标类子类，通过方法重写增强目标方法，所以 final 类、final 方法和 private 方法
不适合增强。

Spring AOP 的事务、日志、指标都依赖代理调用。排查 AOP 不生效时，我会先看代理类型、调用路径、
方法可见性和是否存在自调用。
```

## 回答评分点

高分答案应该覆盖：

- JDK 代理基于接口。
- CGLIB 基于继承。
- final/private 限制。
- Spring AOP 依赖代理。
- 能联系事务不生效排查。

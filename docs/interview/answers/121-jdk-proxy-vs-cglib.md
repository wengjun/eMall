# 121 JDK 动态代理和 CGLIB 有什么区别？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

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

## 深度完善：面向 L6 的回答框架

围绕「JDK 动态代理和 CGLIB 有什么区别？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
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

重复补强内容已合并到 [面试补强共享框架](../deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：JDK 动态代理和 CGLIB 有什么区别？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

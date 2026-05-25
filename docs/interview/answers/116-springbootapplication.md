# 116 @SpringBootApplication 包含哪些注解？

[返回按分类学习面试题](../README.md)

## 题目

`@SpringBootApplication` 包含哪些注解？

## 先给面试官的短答案

`@SpringBootApplication` 是组合注解，核心包含 `@SpringBootConfiguration`、`@EnableAutoConfiguration`
和 `@ComponentScan`。它表示当前类是 Spring Boot 配置类，启用自动配置，并从当前包开始扫描组件。

理解它有助于排查 Bean 扫描不到、自动配置不生效和包结构不合理问题。

## 三个核心注解

核心包括：

- `@SpringBootConfiguration`。
- `@EnableAutoConfiguration`。
- `@ComponentScan`。

它们共同完成启动配置、自动装配和组件扫描。

## SpringBootConfiguration

`@SpringBootConfiguration` 本质上是特殊的 `@Configuration`。

它说明当前类是配置类，可以定义 Bean。

一个应用通常只有一个主启动配置类。

## EnableAutoConfiguration

`@EnableAutoConfiguration` 启用 Spring Boot 自动配置。

它会根据 classpath、配置属性和已有 Bean 自动创建默认 Bean。

例如引入 Web 依赖后自动配置 MVC、Tomcat、Jackson 等。

## ComponentScan

`@ComponentScan` 从当前启动类所在包开始扫描组件。

会扫描：

- `@Component`。
- `@Service`。
- `@Repository`。
- `@Controller`。
- `@RestController`。
- `@Configuration`。

如果启动类包位置太深，其他包下 Bean 可能扫描不到。

## 包结构建议

启动类应放在业务根包。

例如：

```text
com.emall.order.OrderApplication
com.emall.order.application
com.emall.order.domain
com.emall.order.infrastructure
```

这样默认扫描能覆盖模块内部组件。

## 常见问题

问题：

- Bean 扫描不到。
- Mapper 未扫描。
- 自动配置被排除。
- 多个启动类包结构混乱。
- 测试启动上下文不完整。

排查时先看启动类位置和 scan base packages。

## 在 eMall 项目中怎么讲？

每个 eMall 微服务模块的启动类应位于模块根包，例如 `com.emall.payment`。

公共组件放在 `common` 时，要通过 starter、显式扫描或自动配置方式引入，不能依赖随意扩大扫描范围。

否则模块边界会变混乱。

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
@SpringBootApplication 是组合注解，核心是 @SpringBootConfiguration、@EnableAutoConfiguration
和 @ComponentScan。它既声明启动类是配置类，又启用自动配置，还从启动类所在包开始扫描组件。

工程上我会把启动类放在模块根包，避免扫描不到 Bean；公共能力用 starter 或自动配置引入，而不是无限扩大扫描范围。
```

## 回答评分点

高分答案应该覆盖：

- 三个核心注解。
- 自动配置和组件扫描作用。
- 启动类包位置影响扫描。
- 公共组件不应靠乱扫。

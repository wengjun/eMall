# 115 Spring Boot 自动配置原理是什么？

[返回按分类学习面试题](../README.md)

## 题目

Spring Boot 自动配置原理是什么？

## 先给面试官的短答案

Spring Boot 自动配置的核心是根据 classpath、配置属性和已有 Bean 条件，自动创建合适的 Bean。
它通过 `@EnableAutoConfiguration` 导入自动配置类，并使用大量 `@ConditionalOnClass`、
`@ConditionalOnMissingBean`、`@ConditionalOnProperty` 等条件注解控制是否生效。

自动配置不是魔法，本质是条件化的 Spring 配置。

## 自动配置解决什么问题？

传统 Spring 项目需要手动配置大量 Bean。

例如：

- Web MVC。
- Jackson。
- DataSource。
- TransactionManager。
- RedisTemplate。
- Actuator。

Spring Boot 根据依赖和配置自动装配默认 Bean，让项目快速启动。

## 入口注解

常见入口：

```java
@SpringBootApplication
public class Application {
}
```

其中包含 `@EnableAutoConfiguration`。

它会导入 Spring Boot 提供的自动配置类。

## 条件注解

常见条件：

- `@ConditionalOnClass`：classpath 存在某个类。
- `@ConditionalOnMissingBean`：容器中没有某个 Bean。
- `@ConditionalOnBean`：容器中已有某个 Bean。
- `@ConditionalOnProperty`：配置项满足条件。
- `@ConditionalOnWebApplication`：当前是 Web 应用。

这些条件决定自动配置是否生效。

## 用户配置优先

自动配置通常使用 `@ConditionalOnMissingBean`。

含义是：如果用户自己定义了 Bean，Spring Boot 就不再创建默认 Bean。

这让默认配置可覆盖。

## 配置属性

自动配置通常结合 `@ConfigurationProperties`。

例如：

```text
server.port=8080
spring.datasource.url=...
```

配置属性绑定到配置类，再用于创建 Bean。

## 排查自动配置

可以用：

- Actuator conditions endpoint。
- 启动参数 `--debug`。
- 查看自动配置报告。
- 查看 Bean 定义。

当自动配置不符合预期时，不要猜，要看条件是否匹配。

## 在 eMall 项目中怎么讲？

eMall 使用 Spring Boot 启动多个微服务。

数据源、Web、Validation、Actuator、HTTP client、MyBatis Plus 等都可以通过自动配置减少样板代码。

但核心交易组件要明确覆盖默认配置，例如线程池、连接池、超时和序列化策略。

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
Spring Boot 自动配置本质是条件化配置。@SpringBootApplication 间接启用 @EnableAutoConfiguration，
Spring Boot 导入一批自动配置类，再通过 ConditionalOnClass、ConditionalOnMissingBean、
ConditionalOnProperty 等条件判断是否创建 Bean。用户自定义 Bean 通常优先于默认 Bean。

生产中我会利用自动配置减少样板，但对线程池、连接池、超时、序列化和安全配置显式覆盖。
```

## 回答评分点

高分答案应该覆盖：

- 自动配置是条件化 Bean 创建。
- `@EnableAutoConfiguration`。
- 常见 Conditional 注解。
- 用户 Bean 可覆盖默认 Bean。
- 会用条件报告排查。

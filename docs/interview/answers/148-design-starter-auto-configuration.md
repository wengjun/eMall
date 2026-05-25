# 148 如何设计 starter 或 auto-configuration？

[返回按分类学习面试题](../README.md)

## 题目

如何设计 starter 或 auto-configuration？

## 先给面试官的短答案

starter 用来封装公共依赖和默认配置，auto-configuration 用条件化 Bean 创建公共能力。
设计时要提供清晰的 properties、合理默认值、条件注解、用户可覆盖 Bean、最小依赖、自动配置元数据和测试。

好的 starter 应该开箱即用，但不绑死业务。

## starter 包含什么？

通常包含：

- 依赖声明。
- 自动配置类。
- properties 配置类。
- 默认 Bean。
- 条件注解。
- 文档和示例。

starter 不应该包含业务强耦合逻辑。

## 条件化配置

常用条件：

- `@ConditionalOnClass`。
- `@ConditionalOnMissingBean`。
- `@ConditionalOnProperty`。
- `@ConditionalOnBean`。

这样只有在依赖和配置满足条件时才创建 Bean。

## 用户可覆盖

公共 starter 不应强行覆盖用户配置。

通常使用：

```java
@ConditionalOnMissingBean
```

用户自定义 Bean 时，默认 Bean 自动退让。

## 配置属性

用 `@ConfigurationProperties` 暴露配置。

要求：

- 命名清晰。
- 默认值安全。
- 有说明文档。
- 支持配置元数据。
- 避免过度动态化。

## 测试

starter 要测试：

- 条件满足时 Bean 创建。
- 条件不满足时不创建。
- 用户 Bean 能覆盖默认 Bean。
- properties 能正确绑定。
- 多模块使用不冲突。

## 在 eMall 项目中怎么讲？

eMall 可以把统一 HTTP client、trace 透传、错误码、限流、熔断、审计日志封装成 starter。

业务模块引入 starter 后获得默认能力，但仍可覆盖超时、线程池和降级策略。

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
starter 负责封装依赖和开箱即用能力，auto-configuration 负责按条件创建 Bean。设计时要用
ConditionalOnClass、ConditionalOnProperty、ConditionalOnMissingBean 等控制生效条件，用
ConfigurationProperties 暴露配置，并保证用户可覆盖默认 Bean。

公共 starter 要最小依赖、默认安全、文档清晰、有自动配置测试，不能把业务逻辑硬塞进去。
```

## 回答评分点

高分答案应该覆盖：

- starter 封装公共能力。
- auto-configuration 条件化创建 Bean。
- 用户可覆盖。
- properties 和默认值。
- 自动配置测试。

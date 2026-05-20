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

## 深度完善：面向 L6 的回答框架

围绕「如何设计 starter 或 auto-configuration？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
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
本题复习重点：如何设计 starter 或 auto-configuration？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

# 132 Spring 配置加载优先级是什么？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

Spring 配置加载优先级是什么？

## 先给面试官的短答案

Spring Boot 配置有一套明确优先级，常见高优先级来源包括命令行参数、环境变量、系统属性、外部配置文件，
低优先级包括 jar 包内的 `application.yml` 和默认值。相同配置项通常高优先级覆盖低优先级。

生产排查配置问题时，要先确认配置来自哪里，以及最终生效值是什么。

## 常见配置来源

常见来源：

- 命令行参数。
- Java system properties。
- 操作系统环境变量。
- 外部 `application.yml`。
- jar 包内 `application.yml`。
- profile 专属配置。
- 配置中心。
- 默认配置。

不同版本 Spring Boot 细节可能略有差异，但覆盖原则一致。

## 为什么优先级重要？

同一个配置可能出现在多个地方。

例如：

```text
server.port=8080
```

如果环境变量、命令行和配置文件都设置了，最终以高优先级为准。

不知道优先级会导致“我明明改了配置但没生效”的问题。

## profile 配置

profile 用于区分环境：

```text
application-dev.yml
application-prod.yml
```

激活方式：

```text
spring.profiles.active=prod
```

生产环境不应依赖开发 profile 默认值。

## 配置中心

配置中心引入后，还要明确：

- 加载时机。
- 本地配置和远程配置谁覆盖谁。
- 是否支持动态刷新。
- 刷新后哪些 Bean 生效。
- 配置变更是否审计。

关键配置不能随意动态修改。

## 如何排查最终值？

可以使用：

- Actuator env endpoint。
- Actuator configprops endpoint。
- 启动日志。
- 配置中心发布记录。
- 容器环境变量。

生产上暴露这些端点要做好权限控制。

## 在 eMall 项目中怎么讲？

订单服务的数据库连接、线程池大小、下游超时、熔断阈值都可能来自不同配置源。

如果线上超时配置不符合预期，要查配置中心、环境变量、profile 和启动参数的最终覆盖关系。

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
Spring Boot 配置来自命令行、系统属性、环境变量、外部配置文件、包内配置、profile、配置中心和默认值。
相同 key 通常高优先级覆盖低优先级。生产排查配置问题不能只看代码仓库里的 yml，而要看最终运行环境的
effective config。

我会用 Actuator env/configprops、配置中心发布记录和容器环境变量确认最终生效值。
```

## 回答评分点

高分答案应该覆盖：

- 配置有优先级。
- 高优先级覆盖低优先级。
- profile 会影响配置。
- 配置中心要看加载和刷新。
- 会用 Actuator 排查最终值。

## 深度完善：面向 L6 的回答框架

围绕「Spring 配置加载优先级是什么？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
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

本题复习重点：Spring 配置加载优先级是什么？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。


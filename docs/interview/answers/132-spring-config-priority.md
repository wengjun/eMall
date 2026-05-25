# 132 Spring 配置加载优先级是什么？

[返回按分类学习面试题](../README.md)

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

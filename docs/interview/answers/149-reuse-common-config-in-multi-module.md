# 149 如何在多模块项目中复用公共配置？

[返回按分类学习面试题](../README.md)

## 题目

如何在多模块项目中复用公共配置？

## 先给面试官的短答案

多模块项目复用公共配置要区分构建配置、代码配置和运行配置。构建配置放父 POM 的 dependencyManagement 和 pluginManagement；
代码公共能力放 common 或 starter；运行配置通过 profile、配置中心和环境变量管理。不要让业务模块互相复制配置。

公共配置要可覆盖、可测试、版本可控。

## 构建配置

父 POM 管理：

- Java 版本。
- 依赖版本。
- Maven 插件版本。
- Checkstyle。
- Surefire/Failsafe。
- 编译参数。

子模块只声明需要什么，不重复声明版本。

## 代码公共配置

公共代码可以放：

- common。
- shared library。
- Spring Boot starter。
- auto-configuration。

适合封装：

- 统一异常。
- 统一响应。
- HTTP client。
- trace 透传。
- 安全配置。
- MyBatis Plus 配置。

## 运行配置

运行配置不应简单复制到每个模块。

应通过：

- `application.yml` 基线。
- profile。
- 配置中心。
- 环境变量。
- Kubernetes ConfigMap/Secret。

不同环境和服务可以覆盖自己的值。

## 避免过度公共化

不是所有配置都应该公共。

公共配置适合稳定横切能力。

业务特定配置应留在业务模块。

过度抽取会导致公共模块变成“大杂烩”。

## 版本治理

公共配置升级要注意：

- 语义化版本。
- 向后兼容。
- 变更说明。
- 灰度升级。
- 回滚路径。

公共库影响多个服务，不能随意破坏。

## 在 eMall 项目中怎么讲？

eMall 的父 POM 管理依赖和插件版本。

common 提供错误码、响应体、审计、基础异常。

统一 HTTP client 和 trace 透传可以做成 starter，由订单、库存、支付等模块引入。

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
多模块复用公共配置要分层：父 POM 管理依赖和插件版本，common 或 starter 封装公共代码和自动配置，
运行期配置通过 profile、配置中心和环境变量管理。公共能力要可覆盖、可测试、可版本化。

我会避免把业务特定配置放进 common，公共模块只承载稳定横切能力，否则会变成高耦合大杂烩。
```

## 回答评分点

高分答案应该覆盖：

- 父 POM 管理构建配置。
- common/starter 管理公共代码配置。
- 运行配置用 profile/配置中心/环境变量。
- 公共配置要可覆盖。
- 避免过度公共化。

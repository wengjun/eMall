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

## 深度完善：面向 L6 的回答框架

围绕「如何在多模块项目中复用公共配置？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
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

本题复习重点：如何在多模块项目中复用公共配置？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

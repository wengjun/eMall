# 128 Controller、Service、Repository 的职责边界是什么？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

Controller、Service、Repository 的职责边界是什么？

## 先给面试官的短答案

Controller 负责协议适配和参数校验，Service 负责编排业务用例和事务边界，Repository 负责数据访问和持久化抽象。
Controller 不应写业务规则，Repository 不应编排业务流程，Service 不应堆成上帝类。

清晰边界能提高可测试性、可维护性和模块演进能力。

## Controller

Controller 职责：

- 接收 HTTP 请求。
- 解析参数。
- 基础格式校验。
- 调用应用服务。
- 转换响应。
- 处理协议状态码。

不应该：

- 写复杂业务逻辑。
- 直接操作数据库。
- 调用多个 Repository 编排事务。

## Service

Service 职责：

- 表达业务用例。
- 编排领域对象和外部依赖。
- 控制事务边界。
- 做权限和业务校验。
- 发布领域事件或应用事件。

Service 应该面向业务动作，例如 `createOrder`、`payOrder`、`cancelOrder`。

## Repository

Repository 职责：

- 封装数据访问。
- 隐藏 SQL 或 ORM 细节。
- 提供领域语义查询。
- 保存和加载聚合或实体。

不应该：

- 调用远程服务。
- 编排业务流程。
- 处理 HTTP 参数。

## DTO 和领域对象

Controller 接收的是 request DTO。

Service 使用命令对象或领域对象。

Repository 返回实体或持久化对象。

不要让前端 DTO 穿透到所有层，否则协议变化会污染业务层。

## 在 eMall 项目中怎么讲？

订单创建：

- Controller 接收 `CreateOrderRequest`。
- Service 执行创建订单用例，校验用户、库存、价格和幂等。
- Repository 保存订单和查询订单。

如果 Controller 直接扣库存、算价格和写订单，就会变成胖 Controller，难测试也难复用。

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
Controller 是协议适配层，负责 HTTP 参数、基础校验和响应转换；Service 是应用用例层，负责编排业务、
事务边界和调用领域能力；Repository 是数据访问抽象，负责持久化和查询。DTO 不应随意穿透所有层。

边界清晰后，Controller 可以薄，Service 可以按用例测试，Repository 可以替换实现，系统更容易演进。
```

## 回答评分点

高分答案应该覆盖：

- Controller 做协议适配。
- Service 做业务用例和事务。
- Repository 做数据访问。
- 防止胖 Controller 和上帝 Service。
- DTO 不应污染所有层。

## 深度完善：面向 L6 的回答框架

围绕「Controller、Service、Repository 的职责边界是什么？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
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

本题复习重点：Controller、Service、Repository 的职责边界是什么？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

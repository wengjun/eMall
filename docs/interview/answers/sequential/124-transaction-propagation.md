# 124 事务传播行为有哪些？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

事务传播行为有哪些？

## 先给面试官的短答案

事务传播行为定义一个事务方法被另一个事务方法调用时，应该加入现有事务、创建新事务、挂起事务还是非事务执行。
Spring 常见传播行为包括 `REQUIRED`、`REQUIRES_NEW`、`NESTED`、`SUPPORTS`、`NOT_SUPPORTED`、
`MANDATORY` 和 `NEVER`。

最常用的是 `REQUIRED`，最容易误用的是 `REQUIRES_NEW` 和 `NESTED`。

## REQUIRED

默认传播行为。

如果当前有事务，就加入当前事务。

如果没有事务，就新建事务。

适合大多数业务写操作。

## REQUIRES_NEW

总是新建一个事务。

如果当前已有事务，会挂起当前事务。

适合：

- 独立审计日志。
- 独立操作记录。
- 主事务失败也希望保留的记录。

但它会额外占用数据库连接，不能滥用。

## NESTED

在已有事务中创建嵌套事务，通常依赖 savepoint。

内部事务回滚可以回到保存点，不一定回滚整个外部事务。

前提是事务管理器和数据库支持保存点。

## SUPPORTS

如果有事务就加入。

如果没有事务就非事务执行。

适合查询方法，但实际项目中查询是否需要只读事务要按一致性需求判断。

## NOT_SUPPORTED

总是非事务执行。

如果当前有事务，会挂起当前事务。

适合不希望被长事务包住的非事务操作。

## MANDATORY

必须在已有事务中运行。

如果当前没有事务，抛异常。

适合强制某些内部方法只能在事务上下文中调用。

## NEVER

必须在非事务中运行。

如果当前有事务，抛异常。

使用较少。

## 在 eMall 项目中怎么讲？

订单创建主流程通常使用 `REQUIRED`。

审计日志如果希望主事务回滚后仍保留，可以使用 `REQUIRES_NEW`，但要注意连接池压力。

不要用 `REQUIRES_NEW` 包远程调用，也不要用传播行为掩盖事务边界混乱。

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
事务传播行为定义事务方法相互调用时如何处理事务上下文。REQUIRED 是默认，有事务加入、无事务创建；
REQUIRES_NEW 总是创建新事务并挂起外部事务；NESTED 使用保存点做嵌套回滚；SUPPORTS 有事务就加入；
NOT_SUPPORTED 挂起事务非事务执行；MANDATORY 要求必须有事务；NEVER 要求不能有事务。

生产中我最关注传播行为是否符合业务边界，以及 REQUIRES_NEW 是否造成额外连接和一致性风险。
```

## 回答评分点

高分答案应该覆盖：

- 能说出七种传播行为。
- `REQUIRED` 是默认。
- `REQUIRES_NEW` 挂起外部事务。
- `NESTED` 依赖 savepoint。
- 能联系连接池和业务边界。

## 深度完善：面向 L6 的回答框架

围绕「事务传播行为有哪些？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
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

本题复习重点：事务传播行为有哪些？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

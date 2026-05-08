# 122 @Transactional 为什么有时不生效？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

`@Transactional` 为什么有时不生效？

## 先给面试官的短答案

`@Transactional` 不生效通常是因为调用没有经过 Spring 代理、方法不可代理、异常类型不触发回滚、
事务管理器不匹配、数据库操作不在同一事务资源中，或者事务被错误捕获。最常见的是同类自调用绕过代理。

排查时要先确认目标方法是否由 Spring 管理，并且外部调用是否经过代理对象。

## 自调用绕过代理

同一个类内部调用事务方法：

```java
public void outer() {
    inner();
}

@Transactional
public void inner() {
}
```

`inner()` 是 `this.inner()`，不会经过代理对象，因此事务切面不执行。

这是最常见原因。

## 方法不可代理

可能原因：

- private 方法。
- final 方法。
- final 类。
- 静态方法。
- 非 Spring Bean。

Spring AOP 基于代理，不能增强所有调用形式。

## 异常类型问题

默认情况下，Spring 事务对 unchecked exception 和 `Error` 回滚。

checked exception 默认不回滚。

如果需要 checked exception 回滚，要配置：

```java
@Transactional(rollbackFor = Exception.class)
```

如果异常被 catch 后吞掉，事务也可能正常提交。

## 事务管理器问题

多数据源时可能存在多个事务管理器。

如果使用了错误事务管理器，事务可能没有作用到目标数据源。

要明确：

- 使用哪个 `PlatformTransactionManager`。
- Mapper 或 Repository 连接哪个数据源。
- 是否跨库。

## 数据库和引擎问题

事务还依赖数据库能力。

例如 MySQL 中 MyISAM 不支持事务。

即使 Spring 开启事务，底层数据库不支持也无法回滚。

## 异步线程问题

事务上下文通常绑定当前线程。

如果事务方法里启动新线程或异步任务，新线程不会自动继承当前事务。

这会导致异步写库不在原事务内。

## 在 eMall 项目中怎么讲？

订单创建中，如果 `createOrder()` 内部直接调用同类 `deductInventoryInTx()`，后者事务可能不生效。

更合理的是把事务边界放在外部可代理的应用服务方法，或者拆到另一个 Spring Bean 中。

同时避免事务里调用远程库存或支付服务。

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
@Transactional 不生效最常见原因是调用没有经过 Spring 代理，比如同类自调用。其他原因包括 private/final
方法不可代理、目标对象不是 Spring Bean、异常被吞掉、checked exception 默认不回滚、多事务管理器选错、
异步线程丢失事务上下文，以及底层数据库不支持事务。

我排查时会先确认 Bean 是否被代理、调用路径是否经过代理、异常是否触发回滚，以及数据源和事务管理器是否匹配。
```

## 回答评分点

高分答案应该覆盖：

- 自调用绕过代理。
- private/final/static 等不可代理。
- checked exception 默认不回滚。
- 异常被 catch 会影响回滚。
- 多数据源事务管理器要匹配。

## 深度完善：面向 L6 的回答框架

围绕「@Transactional 为什么有时不生效？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
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

本题复习重点：`@Transactional` 为什么有时不生效？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

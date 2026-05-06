# 123 自调用为什么绕过事务代理？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

自调用为什么绕过事务代理？

## 先给面试官的短答案

Spring 声明式事务基于 AOP 代理。只有外部通过代理对象调用事务方法时，事务拦截器才有机会执行。
同一个类内部方法调用本质是 `this.method()`，直接调用目标对象自身，不经过代理对象，因此事务切面不会触发。

解决方式是调整调用边界、拆分 Bean、使用编程式事务，或在极少数情况下通过代理对象调用。

## 代理调用路径

正常事务调用路径：

```text
caller -> proxy -> transaction interceptor -> target method
```

代理在目标方法前开启事务，方法执行后提交或回滚。

## 自调用路径

同类内部调用：

```text
target outer -> this.inner()
```

没有经过代理。

事务拦截器没有机会执行。

所以 `inner()` 上的 `@Transactional` 不生效。

## 示例

```java
@Service
public class OrderService {
    public void create() {
        saveInTransaction();
    }

    @Transactional
    public void saveInTransaction() {
        repository.save();
    }
}
```

`create()` 调用 `saveInTransaction()` 是自调用。

如果外部调用的是 `create()`，`saveInTransaction()` 的事务不会按预期开启。

## 推荐解决方式

更推荐：

- 把事务放到外部入口方法。
- 把事务方法拆到另一个 Spring Bean。
- 使用 `TransactionTemplate`。
- 重构服务职责。

不推荐为了绕过问题滥用 `AopContext.currentProxy()`。

## 为什么不建议暴露代理？

通过当前代理调用会让业务代码依赖 Spring AOP 细节。

这会降低可测试性和可维护性。

通常说明事务边界设计不清楚。

## 在 eMall 项目中怎么讲？

订单服务中应把一次本地数据库状态变更设计成明确事务边界。

例如 `OrderApplicationService.createOrder()` 作为事务入口。

内部领域方法不依赖 `@Transactional` 自调用，而是由应用服务统一控制事务。

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
自调用绕过事务代理，是因为 Spring 事务基于代理，事务拦截器只在调用经过代理对象时执行。
同一个类内部调用是 this.method()，直接进入目标对象，不经过 proxy，所以 @Transactional 不触发。

我会通过调整事务入口、拆分 Bean 或使用 TransactionTemplate 解决，而不是让业务代码强依赖 AopContext。
```

## 回答评分点

高分答案应该覆盖：

- Spring 事务基于代理。
- 自调用是 `this.method()`。
- 事务拦截器不执行。
- 推荐拆 Bean 或调整事务边界。
- 不建议滥用当前代理。

## 深度完善：面向 L6 的回答框架

围绕「自调用为什么绕过事务代理？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
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

本题复习重点：自调用为什么绕过事务代理？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。


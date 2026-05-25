# 122 @Transactional 为什么有时不生效？

[返回按分类学习面试题](../README.md)

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

# 123 自调用为什么绕过事务代理？

[返回按分类学习面试题](../README.md)

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

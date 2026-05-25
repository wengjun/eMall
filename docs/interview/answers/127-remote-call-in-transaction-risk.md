# 127 事务里调用远程服务有什么风险？

[返回按分类学习面试题](../README.md)

## 题目

事务里调用远程服务有什么风险？

## 先给面试官的短答案

事务里调用远程服务会拉长数据库事务时间，增加锁持有时间、连接占用、死锁概率和 P99 延迟。
远程调用还可能超时、失败或产生不确定结果，导致本地事务和远程副作用不一致。

生产中应避免在数据库事务中调用 HTTP/RPC，优先使用本地事务加事件、Outbox、状态机和补偿。

## 风险一：长事务

远程调用耗时不可控。

如果事务里调用下游：

- 数据库连接长时间占用。
- 行锁长时间持有。
- 其他请求等待。
- 死锁概率增加。

高峰期会明显放大 P99。

## 风险二：远程结果不确定

远程调用可能：

- 超时。
- 失败。
- 成功但响应丢失。
- 下游处理成功但本地回滚。
- 本地提交但下游失败。

这会造成分布式一致性问题。

## 风险三：事务无法覆盖远程副作用

本地数据库事务不能回滚远程服务已经完成的操作。

例如本地订单回滚，但支付服务已经扣款。

这需要补偿，而不是依赖本地事务。

## 推荐方案

常见方案：

- 本地事务只更新本服务数据。
- 写 Outbox 事件。
- 事务提交后异步发送消息。
- 下游消费消息并幂等处理。
- 状态机跟踪流程。
- 失败时补偿或重试。

这是一种最终一致性设计。

## 在 eMall 项目中怎么讲？

创建订单时不应在订单库事务中直接调用支付扣款。

更合理：

- 本地事务创建订单和 outbox 事件。
- 事务提交后发送订单创建事件。
- 支付或库存服务异步处理。
- 订单状态机记录处理中、成功、失败。
- 失败走取消和补偿。

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
事务里调用远程服务会把数据库事务和不可控网络 IO 绑定在一起，导致长事务、锁等待、连接占用、
死锁和 P99 升高。更严重的是远程调用有超时和不确定结果，本地事务无法回滚远程副作用。

我会让本地事务只处理本地状态和 outbox 事件，远程交互通过消息、状态机、幂等和补偿实现最终一致。
```

## 回答评分点

高分答案应该覆盖：

- 长事务和锁等待。
- 连接占用和 P99 风险。
- 远程调用结果不确定。
- 本地事务不能回滚远程副作用。
- 推荐 Outbox、事件、状态机、补偿。

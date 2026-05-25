# 178 如何处理客户端重试导致的重复请求？

[返回按分类学习面试题](../README.md)

## 题目

如何处理客户端重试导致的重复请求？

## 先给面试官的短答案

客户端重试会把“请求一次”变成“请求至少一次”。服务端不能假设请求只到达一次，所有会产生副作用的接口都要做幂等。
常用方案是幂等键、业务唯一约束、状态机校验、请求去重表和结果缓存。

重试策略也要有退避、抖动和最大次数，不能让客户端无限重试。

## 重复请求来源

来源包括：

- 客户端超时后重试。
- 网关超时后重试。
- 网络抖动导致响应丢失。
- 用户重复点击。
- MQ 重复投递。
- 任务调度重复执行。

这些都属于分布式系统的正常现象。

## 服务端幂等

常见方式：

- 使用 `Idempotency-Key`。
- 使用订单号、支付单号等业务唯一键。
- 数据库唯一索引防重复。
- 状态机拒绝非法重复跳转。
- 记录请求处理结果并返回同一结果。
- 对库存、支付等操作做防重和补偿。

幂等键必须和用户、接口、业务参数绑定，不能全局裸用。

## 客户端重试策略

客户端应：

- 只重试可重试错误。
- 设置最大重试次数。
- 使用指数退避。
- 增加随机抖动。
- 不重试明确业务失败。
- 对写接口携带幂等键。

没有幂等保护的写接口不应该鼓励自动重试。

## 在 eMall 项目中怎么讲？

支付创建接口必须使用支付单号或幂等键。

如果客户端第一次请求已经创建支付单，但响应丢失，第二次请求应返回同一个支付单结果，
而不是再次创建一笔支付。

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
客户端重试会导致请求至少一次到达，服务端必须按幂等设计。写接口要使用幂等键、业务唯一约束、状态机和结果缓存，
保证重复请求不会产生重复副作用。

同时客户端重试要受控，只重试超时、网络错误和临时 5xx，使用指数退避和抖动，并限制最大次数。明确业务失败不能重试。
```

## 回答评分点

高分答案应该覆盖：

- 分布式系统中重复请求是正常现象。
- 写接口必须幂等。
- 幂等键要绑定用户、接口和参数。
- 唯一索引和状态机是重要防线。
- 客户端重试要有退避、抖动和次数限制。

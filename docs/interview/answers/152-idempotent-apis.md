# 152 哪些接口应该设计成幂等？

[返回按分类学习面试题](../README.md)

## 题目

哪些接口应该设计成幂等？

## 先给面试官的短答案

所有可能被客户端重试、网关重试、MQ 重投、支付回调重复通知或用户重复点击的写接口，都应该设计成幂等。
典型包括创建订单、支付回调、扣库存、优惠券领取、取消订单、退款、消息消费和任务调度。

幂等的目标是重复请求不会造成重复副作用。

## 为什么需要幂等？

分布式系统中重复很常见：

- 用户重复点击。
- 网络超时后客户端重试。
- 网关重试。
- MQ 至少一次投递。
- 支付渠道重复回调。
- 定时任务重复执行。

如果没有幂等，会出现重复下单、重复扣款、重复发券。

## 天然幂等接口

通常天然幂等：

- `GET` 查询。
- `PUT` 整体替换同一资源。
- `DELETE` 删除同一资源。

但实现仍要注意副作用，例如查询接口不要偷偷写业务状态。

## 需要额外设计幂等的接口

典型：

- `POST /orders`。
- 支付回调。
- 退款申请。
- 优惠券领取。
- 库存预占。
- MQ 消费。
- 后台补偿任务。

这些接口重复执行可能产生副作用。

## 幂等实现方式

常见方式：

- 幂等 key。
- 数据库唯一键。
- 状态机条件更新。
- 幂等表。
- 去重表。
- 业务流水号。
- 乐观锁版本。

幂等要落到持久化约束，不能只靠本地缓存。

## 在 eMall 项目中怎么讲？

创建订单要使用 `clientRequestId` 或购物车结算号做幂等。

支付回调用支付流水号做唯一键。

优惠券领取用 `userId + couponId` 唯一键。

库存扣减用条件更新防止重复和超卖。

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
凡是可能被重试或重复投递的写操作都应幂等，包括创建订单、支付回调、退款、优惠券领取、库存预占、
MQ 消费和补偿任务。幂等不是只防用户重复点击，而是分布式系统可靠性的基础。

实现上我会用幂等 key、唯一键、状态机条件更新和幂等表，让重复请求返回同一业务结果，不产生重复副作用。
```

## 回答评分点

高分答案应该覆盖：

- 重试和重复投递是常态。
- 写接口尤其需要幂等。
- 支付、订单、优惠券、MQ 是重点。
- 幂等要落到持久化约束。
- 重复请求返回同一结果。

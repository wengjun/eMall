# 164 如何做接口限流？

[返回按分类学习面试题](../README.md)

## 题目

如何做接口限流？

## 先给面试官的短答案

接口限流要按维度、算法和层级设计。维度包括用户、IP、appId、商家、接口、租户和全局；
算法包括固定窗口、滑动窗口、令牌桶和漏桶；层级包括网关、服务、线程池和下游保护。
限流后要返回明确错误码，并配合降级、排队和重试退避。

限流是保护系统，不是随便丢请求。

## 限流维度

常见维度：

- IP。
- userId。
- appId。
- merchantId。
- API path。
- tenant。
- region。
- 全局 QPS。

单一维度不够，恶意流量可能绕过。

## 限流算法

常见算法：

- 固定窗口：简单但边界突刺。
- 滑动窗口：更平滑。
- 令牌桶：允许一定突发。
- 漏桶：稳定出流。

秒杀和开放 API 常用令牌桶或滑动窗口。

## 限流层级

层级：

- CDN/WAF。
- API 网关。
- 业务服务。
- 下游 client。
- 数据库或 MQ 入口。

越靠前越能节省资源。

业务服务内部限流用于精细保护。

## 限流响应

被限流应返回：

- HTTP 429。
- 业务错误码。
- retry-after。
- traceId。

客户端应退避重试，而不是立即疯狂重试。

## 在 eMall 项目中怎么讲？

秒杀接口按活动、商品、用户和全局做多级限流。

开放 API 按 appId、商家和接口限流。

下游支付接口按渠道容量限流，防止订单服务把支付打垮。

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
接口限流要设计维度、算法和层级。维度可以是 IP、用户、appId、商家、接口、租户和全局；
算法可选滑动窗口、令牌桶、漏桶；层级包括网关、服务和下游 client。限流响应要用 429、业务错误码和 retry-after。

限流后还要配合降级、排队和客户端退避，避免重试风暴。
```

## 回答评分点

高分答案应该覆盖：

- 多维度限流。
- 令牌桶、漏桶、滑动窗口。
- 网关和服务多层限流。
- 返回 429 和 retry-after。
- 防止重试风暴。

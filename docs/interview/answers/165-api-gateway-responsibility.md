# 165 如何设计 API 网关层职责？

[返回按分类学习面试题](../README.md)

## 题目

如何设计 API 网关层职责？

## 先给面试官的短答案

API 网关应负责通用入口能力，例如路由、认证鉴权、限流、黑白名单、协议转换、trace 注入、日志、灰度、熔断和基础安全防护。
网关不应承载复杂业务逻辑、核心交易规则和数据库访问，否则会变成业务大泥球。

网关是流量入口和治理层，不是业务服务替代品。

## 网关应该做什么？

适合网关：

- 路由转发。
- TLS 终止。
- 认证和 token 校验。
- 粗粒度鉴权。
- 限流。
- WAF/黑白名单。
- trace ID 生成。
- 请求日志。
- 灰度路由。
- 协议转换。

这些是横切入口能力。

## 网关不应该做什么？

不适合网关：

- 复杂订单规则。
- 库存扣减。
- 支付状态机。
- 数据库事务。
- 复杂优惠计算。
- 大量业务聚合。

这些应放在业务服务或 BFF。

## 网关和 BFF 区别

网关偏通用流量治理。

BFF 偏端侧体验聚合和适配。

不要把所有 BFF 聚合逻辑塞进通用网关，否则网关会越来越难维护。

## 可观测性

网关要记录：

- request id。
- trace id。
- app id。
- user id。
- route。
- status。
- latency。
- limit result。

注意敏感信息脱敏。

## 在 eMall 项目中怎么讲？

eMall 网关负责用户认证、限流、灰度和路由。

商品详情页的端侧聚合可以由 BFF 做。

订单创建、库存扣减和支付校验必须在业务服务中完成，不能放在网关。

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
API 网关职责是入口治理，包括路由、认证、粗粒度鉴权、限流、安全防护、trace 注入、日志、灰度和协议适配。
它不应承载复杂业务规则、状态机和数据库事务。复杂端侧聚合应放 BFF，核心业务放后端服务。

网关设计要保持通用、轻量、可观测和高可用，否则入口会成为全站单点风险。
```

## 回答评分点

高分答案应该覆盖：

- 网关做通用入口治理。
- 不做复杂业务。
- 区分网关和 BFF。
- 需要可观测和安全。
- 网关要高可用。

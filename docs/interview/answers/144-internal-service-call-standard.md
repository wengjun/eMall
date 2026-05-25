# 144 如何设计统一的内部服务调用规范？

[返回按分类学习面试题](../README.md)

## 题目

如何设计统一的内部服务调用规范？

## 先给面试官的短答案

内部服务调用规范要统一协议、认证、超时、重试、熔断、错误码、幂等、trace 透传、日志脱敏、版本兼容和监控指标。
规范的目标是让服务调用可治理、可观测、可演进，而不是每个团队各写一套 HTTP 客户端。

## 协议和接口

需要统一：

- HTTP 或 RPC 协议。
- URL 命名。
- 方法语义。
- 请求响应格式。
- 错误响应体。
- API 版本。

内部接口也要稳定，不应随意破坏兼容。

## 超时和重试

必须统一：

- connect timeout。
- read timeout。
- total timeout。
- retry count。
- retry backoff。
- retry jitter。

只有幂等请求才能自动重试。

支付、扣库存等有副作用操作要特别谨慎。

## 安全

内部调用也需要安全：

- 服务身份认证。
- mTLS 或 token。
- 权限校验。
- 请求签名。
- 敏感字段脱敏。

内网不等于安全。

## 可观测性

必须统一：

- trace ID 透传。
- span 命名。
- 调用耗时指标。
- 错误率指标。
- 下游状态码。
- 连接池指标。
- 日志字段。

这样故障时才能快速定位。

## 容错

统一容错：

- 限流。
- 熔断。
- 降级。
- 隔离。
- 超时。
- 舱壁。

每个下游要有明确失败策略。

## 在 eMall 项目中怎么讲？

订单调用库存、支付、风控时，都应该使用统一 client 规范。

例如所有 client 默认带 trace ID、统一错误解码、统一超时和熔断指标。

支付 client 禁止自动重试扣款，只允许查询类接口重试。

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
内部服务调用规范要覆盖协议、认证、超时、重试、熔断、降级、隔离、错误码、幂等、trace 透传、
日志脱敏、版本兼容和指标。重点是把调用治理做成统一基础设施，而不是每个业务自己 new 客户端。

对有副作用接口要明确幂等和重试边界，查询可重试，扣款扣库存不能盲目自动重试。
```

## 回答评分点

高分答案应该覆盖：

- 协议和响应格式统一。
- 超时、重试、熔断统一。
- trace 和指标统一。
- 内部调用也要认证。
- 有副作用操作不能盲目重试。

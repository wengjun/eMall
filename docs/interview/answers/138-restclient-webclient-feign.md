# 138 RestClient、WebClient、Feign 如何取舍？

[返回按分类学习面试题](../README.md)

## 题目

RestClient、WebClient、Feign 如何取舍？

## 先给面试官的短答案

RestClient 是 Spring 新一代同步阻塞 HTTP 客户端，适合 MVC 和普通同步调用；WebClient 是响应式非阻塞客户端，
适合响应式链路、高并发 IO 和流式场景；Feign 是声明式 HTTP 客户端，适合微服务接口调用和统一治理。
选择取决于编程模型、团队规范、调用复杂度和治理能力。

不要为了“新”而混用多个客户端。

## RestClient

RestClient 是 Spring Framework 6 引入的同步 HTTP 客户端。

特点：

- 阻塞式。
- API 比 RestTemplate 更现代。
- 适合 Spring MVC。
- 易理解。

适合大多数同步微服务调用。

## WebClient

WebClient 是响应式客户端。

特点：

- 非阻塞。
- 基于 Reactor。
- 支持流式响应。
- 适合 WebFlux。

如果在 MVC 阻塞链路中使用 WebClient 后又 `.block()`，收益会下降，还会增加复杂度。

## Feign

Feign 是声明式 HTTP 客户端。

特点：

- 接口定义远程调用。
- 适合微服务调用。
- 易集成负载均衡、熔断、重试、日志。
- 调用代码简洁。

但要统一超时、连接池、错误解码和重试策略。

## 如何选择？

建议：

- MVC 同步服务：RestClient 或 Feign。
- 声明式内部服务调用：Feign。
- 响应式端到端链路：WebClient。
- 流式、SSE、大量非阻塞 IO：WebClient。
- 简单外部 API 调用：RestClient。

团队最好统一一种主客户端，避免治理碎片化。

## 生产治理点

无论选哪个，都必须配置：

- 连接池。
- 连接超时。
- 读取超时。
- 总超时。
- 重试策略。
- 熔断降级。
- 日志脱敏。
- trace ID 透传。
- 指标监控。

客户端类型不是生产能力的全部。

## 在 eMall 项目中怎么讲？

eMall 内部服务如果采用 Spring Cloud，可以用 Feign 做声明式调用并统一治理。

网关或高并发流式场景可以用 WebClient。

普通同步外部 HTTP API 可以用 RestClient。

关键是统一规范，避免订单服务里同时混用三套超时和重试策略。

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
RestClient 适合同步阻塞调用，是 RestTemplate 的现代替代；WebClient 是响应式非阻塞客户端，
适合 WebFlux、流式和高并发 IO；Feign 是声明式客户端，适合内部微服务调用和统一治理。

我会按应用编程模型和治理能力选择，而不是追新。无论哪种客户端，都必须统一连接池、超时、重试、熔断、日志和 trace 透传。
```

## 回答评分点

高分答案应该覆盖：

- RestClient 是同步阻塞。
- WebClient 是响应式非阻塞。
- Feign 是声明式。
- 不能为了新而混用。
- 生产治理比客户端类型更重要。

# 143 如何透传 trace ID？

[返回按分类学习面试题](../README.md)

## 题目

如何透传 trace ID？

## 先给面试官的短答案

trace ID 要在入口生成或读取，放入日志上下文和请求上下文，并在所有下游 HTTP、RPC、MQ 消息中透传。
常见做法是使用 OpenTelemetry 或链路追踪框架，通过拦截器、过滤器和消息 header 自动注入和提取。

核心目标是让一次用户请求跨服务可追踪。

## 入口处理

网关或第一个服务需要：

- 从请求 header 读取 trace ID。
- 没有则生成新的 trace ID。
- 放入 MDC。
- 放入 tracing context。
- 响应中可返回 trace ID。

日志必须打印 trace ID。

## HTTP 透传

HTTP 客户端要自动带上追踪 header。

常见标准：

- `traceparent`。
- `tracestate`。
- `baggage`。

不要每个业务手写 header，应该通过拦截器统一处理。

## MQ 透传

异步消息也要透传。

发送消息时把 trace context 写入 message headers。

消费消息时提取 trace context，并创建新的 span。

否则异步链路会断。

## 日志 MDC

日志中要包含：

- traceId。
- spanId。
- userId 或 orderId 这类业务 key。

注意业务 key 要脱敏，不能打印敏感信息。

## 线程切换

异步线程池中 MDC 和 tracing context 可能丢失。

需要使用：

- tracing 框架的 context propagation。
- 包装 Executor。
- 任务提交时复制上下文。

否则异步日志没有 trace ID。

## 在 eMall 项目中怎么讲？

用户创建订单时，trace ID 应从网关透传到订单、库存、支付、风控和消息消费者。

当订单 P99 变高时，可以通过 trace ID 找到慢在库存、支付还是订单本地逻辑。

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
trace ID 透传要在入口生成或提取，写入日志 MDC 和 tracing context，并通过 HTTP/RPC header、MQ message header
传递到所有下游。生产上我会使用 OpenTelemetry 等标准，不让业务手写透传逻辑。

还要处理线程池和异步消息的 context propagation，否则一进入异步任务链路就断了。
```

## 回答评分点

高分答案应该覆盖：

- 入口生成或读取 trace ID。
- HTTP header 透传。
- MQ header 透传。
- 日志 MDC。
- 异步线程上下文传播。

# 143 trace ID 如何生成和透传？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

trace ID 要在入口生成；如果上游传入可信且格式合法的 Trace Context，则继续使用。随后把上下文放入日志和请求作用域，
并在所有下游 HTTP、RPC、MQ 消息中透传。
常见做法是使用 OpenTelemetry 或链路追踪框架，通过拦截器、过滤器和消息 header 自动注入和提取。

核心目标是让一次用户请求跨服务可追踪。

## 入口处理

网关或第一个服务需要：

- 从请求 header 读取 trace ID。
- 没有则生成新的 trace ID。
- 校验外部 trace ID 的长度和字符，不能把它当成认证凭证。
- 放入 MDC。
- 放入 tracing context。
- 响应中可返回 trace ID。

日志必须打印 trace ID。

消息消费者、定时任务等没有 HTTP 上游的入口也要创建新的根 Span。采样可以减少 Span 存储，但不能让错误日志失去
可关联的 trace ID。

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

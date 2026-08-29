# 138 RestClient、WebClient、Feign 如何取舍？

[返回按分类学习面试题](../README.md)

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

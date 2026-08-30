# 157 错误码和 HTTP 状态码如何配合？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

HTTP 状态码表达协议和通用语义，业务错误码表达具体业务原因。比如参数错误用 400 加 `VALIDATION_FAILED`，
库存不足可以用 409 或 200 加业务失败码，系统异常用 500 加 `SYSTEM_ERROR`。关键是团队规范一致，
调用方能稳定处理。

不要只用 HTTP 200 承载所有错误，也不要只靠 HTTP 状态码表达复杂业务原因。

## HTTP 状态码职责

HTTP 状态码适合表达：

- 请求格式错误。
- 未认证。
- 无权限。
- 资源不存在。
- 冲突。
- 限流。
- 服务异常。

它是协议层标准。

## 业务错误码职责

业务错误码表达具体业务语义。

例如：

- `ORDER_NOT_FOUND`。
- `INVENTORY_NOT_ENOUGH`。
- `COUPON_EXPIRED`。
- `PAYMENT_DUPLICATED`。
- `ORDER_STATUS_NOT_CANCELABLE`。

这些无法仅靠 HTTP 状态码准确表达。

## 常见映射

常见搭配：

- 400 + `VALIDATION_FAILED`。
- 401 + `UNAUTHENTICATED`。
- 403 + `FORBIDDEN`。
- 404 + `ORDER_NOT_FOUND`。
- 409 + `ORDER_STATUS_CONFLICT`。
- 429 + `RATE_LIMITED`。
- 500 + `SYSTEM_ERROR`。

业务可预期失败要和系统异常区分。

## 关于全 200

有些团队所有响应都返回 HTTP 200，再用业务 code 判断。

这对某些网关或老客户端简单，但会弱化 HTTP 语义。

对开放 API 和内部微服务，更推荐合理使用 HTTP 状态码。

## 电商系统实践

库存不足可以视为业务冲突，返回 409 和 `INVENTORY_NOT_ENOUGH`。

参数 quantity 小于 1 返回 400 和 `VALIDATION_FAILED`。

数据库异常返回 500 和 `SYSTEM_ERROR`，并带 traceId。

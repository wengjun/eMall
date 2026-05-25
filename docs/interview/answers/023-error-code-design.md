# 023 如何设计统一错误码？

[返回按分类学习面试题](../README.md)

## 题目

如何设计统一错误码？

## 先给面试官的短答案

统一错误码要稳定、可分类、可监控、可被前端和客服理解。它不是简单给异常起名字，
而是 API 契约、排障入口和业务指标的一部分。

我会按错误类型分层，例如参数错误、认证失败、权限不足、资源不存在、状态冲突、
限流、下游不可用、系统错误。HTTP 状态码表达协议层结果，业务错误码表达业务原因。

## 为什么需要统一错误码？

如果每个接口随便返回字符串：

```text
order not found
Order missing
no such order
```

前端无法稳定处理，监控也无法聚合。

统一错误码可以用于：

- 前端展示。
- 客服定位。
- 日志检索。
- 指标聚合。
- 告警规则。
- API 契约。
- 国际化文案映射。

## 推荐分类

常见错误码：

```text
BAD_REQUEST
UNAUTHORIZED
FORBIDDEN
NOT_FOUND
CONFLICT
TOO_MANY_REQUESTS
DOWNSTREAM_UNAVAILABLE
SYSTEM_BUSY
INTERNAL_ERROR
```

业务更细时可以按领域扩展：

```text
INSUFFICIENT_STOCK
ORDER_STATUS_CONFLICT
PAYMENT_AMOUNT_MISMATCH
COUPON_NOT_AVAILABLE
```

关键是不要无限膨胀。错误码越多，治理成本越高。

## HTTP 状态码和业务错误码如何配合？

HTTP 状态码表达协议层语义：

- 400：请求参数问题。
- 401：未认证。
- 403：无权限。
- 404：资源不存在。
- 409：业务状态冲突。
- 429：限流。
- 503：下游不可用或系统繁忙。
- 500：未预期系统错误。

业务错误码表达具体原因：

```json
{
  "code": "PAYMENT_AMOUNT_MISMATCH",
  "message": "Payment amount mismatch"
}
```

## 错误响应结构

推荐：

```json
{
  "success": false,
  "code": "ORDER_STATUS_CONFLICT",
  "message": "Order cannot be paid from CANCELLED",
  "traceId": "..."
}
```

生产中 `message` 要避免泄露内部细节。更安全的方式是返回用户可理解信息，
详细原因写日志。

## 错误码设计原则

- 稳定，不随便改名。
- 可分类。
- 可监控。
- 文案和 code 分离。
- 不暴露敏感内部实现。
- 文档化。
- 有测试覆盖。
- 老客户端能处理未知错误码。

## 在 eMall 项目中怎么讲？

eMall 中统一 `ErrorCode` 可以支持：

- `BusinessException` 携带错误码。
- `CommonExceptionHandlerSupport` 统一映射 HTTP 状态。
- 指标按错误码统计。
- 前端按错误码展示文案。
- 运维通过 traceId 和错误码排查。

例如：

```java
throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid");
```

## 常见追问

### 错误码越细越好吗？

不是。太粗无法定位，太细难治理。高频、需要前端特殊处理、需要监控聚合的错误才值得独立错误码。

### message 能不能给用户看？

业务错误可以给用户友好文案。系统错误不要暴露内部细节。

## 专家级完整回答

```text
统一错误码是 API 契约和可观测性的一部分。我会让 HTTP 状态码表达协议层结果，
业务错误码表达具体业务原因，例如库存不足、订单状态冲突、支付金额不一致。

错误码要稳定、可分类、可监控，不能随意改名。响应中返回 code、message 和 traceId，
完整异常堆栈留在服务端日志。前端和多语言文案基于 code 映射，监控也按 code 聚合错误趋势。
```

## 回答评分点

高分答案应该覆盖：

- 错误码是契约和观测维度。
- 能区分 HTTP 状态码和业务错误码。
- 能设计响应结构。
- 能说明稳定性、分类、监控、国际化。
- 能说明错误码不能无限膨胀。

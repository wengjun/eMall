# 156 如何设计统一响应体？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

统一响应体通常包含业务错误码、消息、数据、traceId、时间戳和可选错误详情。HTTP 状态码表达协议层结果，
业务 code 表达业务处理结果。设计要稳定、简洁、可扩展，并避免把内部异常堆栈返回给客户端。

统一响应体的目标是让前端、客户端和调用方一致处理成功和失败。

## 基本结构

示例：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {},
  "traceId": "abc",
  "timestamp": "2026-04-30T00:00:00Z"
}
```

失败时：

```json
{
  "code": "ORDER_INVENTORY_NOT_ENOUGH",
  "message": "Inventory is not enough",
  "traceId": "abc"
}
```

## HTTP 状态码和业务 code

HTTP 状态码表达协议层：

- 200：请求成功处理。
- 400：参数错误。
- 401/403：认证授权失败。
- 404：资源不存在。
- 409：冲突。
- 429：限流。
- 500：服务内部错误。

业务 code 表达业务结果。

两者不要混用。

## 错误详情

参数校验可以返回字段级错误：

```json
{
  "code": "VALIDATION_FAILED",
  "details": [
    {"field": "quantity", "reason": "must be greater than 0"}
  ]
}
```

但不要返回内部类名、SQL、堆栈或密钥。

## 兼容性

响应体要考虑：

- 新增字段兼容。
- 字段命名稳定。
- 错误码不随意改语义。
- data 类型明确。
- 时间格式统一。

客户端应该能忽略未知字段。

## 在 eMall 项目中怎么讲？

订单库存不足返回业务错误码 `ORDER_INVENTORY_NOT_ENOUGH`。

系统异常返回 `SYSTEM_ERROR` 和 traceId，服务端日志中记录堆栈。

前端根据 code 做用户提示，根据 traceId 协助排查。

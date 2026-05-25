# 022 为什么不能直接把异常堆栈返回给前端？

[返回按分类学习面试题](../README.md)

## 题目

为什么不能直接把异常堆栈返回给前端？

## 先给面试官的短答案

异常堆栈是内部诊断信息，不是对外 API 契约。直接返回给前端会泄露类名、包名、SQL、
表结构、服务地址、文件路径甚至敏感参数，也会让用户看到无法理解的信息。

正确做法是：前端返回稳定错误码、简短安全提示和 traceId；后端日志记录完整堆栈，
通过 traceId 关联排查。

## 从零基础理解

异常堆栈长这样：

```text
java.lang.NullPointerException
    at com.emall.payment.service.PaymentService.callback(PaymentService.java:54)
    at com.emall.payment.api.PaymentController.callback(PaymentController.java:31)
```

它对研发很有用，但对用户没意义。更糟糕的是，攻击者可以通过堆栈了解系统内部结构。

## 直接返回堆栈的风险

### 泄露内部实现

可能暴露：

- Java 包名和类名。
- 方法名。
- 数据库表名。
- SQL。
- 中间件地址。
- 文件路径。
- 第三方 SDK 信息。

### 泄露敏感信息

异常消息里可能包含：

- 手机号。
- token。
- 签名。
- 地址。
- 支付渠道原始报文。
- 内部密钥片段。

### 破坏用户体验

普通用户看不懂堆栈。前端也无法基于堆栈做稳定逻辑处理。

### 破坏 API 稳定性

堆栈会随代码重构变化，不能作为客户端依赖。

## 正确响应格式

推荐：

```json
{
  "success": false,
  "code": "ORDER_NOT_FOUND",
  "message": "Order not found",
  "traceId": "9f8a..."
}
```

系统异常：

```json
{
  "success": false,
  "code": "INTERNAL_ERROR",
  "message": "System is temporarily unavailable",
  "traceId": "9f8a..."
}
```

用户看到安全提示，研发通过 traceId 查日志。

## 后端日志应该记录什么？

后端日志要记录：

- traceId。
- requestId。
- userId 或脱敏用户标识。
- orderId、paymentId 等业务 ID。
- 错误码。
- 异常堆栈。
- 下游服务名。
- 耗时。

不要记录：

- 明文手机号。
- 明文地址。
- token。
- 密钥。
- 支付完整敏感报文。

## 在 eMall 项目中怎么讲？

eMall 的统一异常处理应把 `BusinessException` 转成统一 `ApiResponse`，
系统异常转成 `INTERNAL_ERROR`，日志里保留堆栈。

这样：

- 前端拿稳定错误码。
- 客服拿 traceId 找研发。
- 研发查日志定位。
- 安全上不泄露内部细节。

## 专家级完整回答

```text
异常堆栈属于内部诊断信息，不能作为对外响应。它会泄露类名、SQL、表名、服务地址、
文件路径和可能的敏感参数，同时前端也无法稳定依赖堆栈。

我会通过统一异常处理返回错误码、用户可理解文案和 traceId。
完整异常堆栈只写入服务端结构化日志，并做敏感信息脱敏。
这样既保证安全和用户体验，也保留了排障能力。
```

## 回答评分点

高分答案应该覆盖：

- 能说明安全泄露风险。
- 能说明用户体验和 API 稳定性问题。
- 能提出错误码、message、traceId 响应。
- 能说明后端日志记录完整堆栈。
- 能强调敏感信息脱敏。

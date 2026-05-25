# 019 Java 异常分为哪些类型？

[返回按分类学习面试题](../README.md)

## 题目

Java 异常分为哪些类型？

## 先给面试官的短答案

Java 异常体系顶层是 `Throwable`，下面主要分为 `Error` 和 `Exception`。
`Error` 通常表示 JVM 或系统级严重问题，业务代码一般不应该捕获后继续运行。
`Exception` 又分 checked exception 和 unchecked exception。

在业务系统中，还会进一步区分参数异常、业务异常、下游异常和系统异常，并通过统一错误码返回。

## 基础结构

```text
Throwable
  Error
  Exception
    RuntimeException
```

### Error

常见：

- `OutOfMemoryError`
- `StackOverflowError`
- `NoClassDefFoundError`

这些通常表示严重问题。业务代码不应该简单 catch 住然后假装恢复。

### checked exception

编译器要求处理。

例如：

- `IOException`
- `SQLException`

方法签名中通常要 `throws`，调用方必须 catch 或继续抛。

### unchecked exception

继承 `RuntimeException`，编译器不强制处理。

例如：

- `NullPointerException`
- `IllegalArgumentException`
- `IllegalStateException`
- 自定义 `BusinessException`

## 业务系统中的异常分类

### 参数异常

用户输入不合法：

- 数量小于 1。
- requestId 为空。
- skuId 格式错误。

通常返回 `400 BAD_REQUEST`。

### 业务异常

请求格式正确，但业务规则不允许：

- 库存不足。
- 订单不存在。
- 已支付订单不能取消。
- 支付金额不一致。

通常返回明确业务错误码。

### 下游异常

调用其他服务失败：

- 库存服务超时。
- 支付渠道不可用。
- Redis 超时。
- Kafka 发送失败。

需要结合重试、熔断、降级、补偿。

### 系统异常

代码或基础设施异常：

- 数据库连接失败。
- 空指针。
- 序列化失败。
- 配置缺失。

需要记录日志和告警。

## 为什么要分层处理？

如果所有异常都返回：

```json
{
  "message": "system error"
}
```

前端、客服、监控、排障都无法判断发生了什么。

如果所有异常都报警，库存不足这种正常业务拒绝也会产生噪音。

正确做法：

- 业务异常有稳定错误码。
- 系统异常记录错误日志和 traceId。
- 下游异常进入熔断、降级、补偿。
- 前端只看到安全、可理解的信息。

## 在 eMall 项目中怎么讲？

eMall 中可以用 `BusinessException` 携带 `ErrorCode`：

```java
throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid");
```

统一异常处理把它转成 API 响应。

这样 Controller 不需要到处 try-catch，错误响应统一，监控也能按错误码聚合。

## 常见追问

### checked exception 和 unchecked exception 哪个更好？

没有绝对。底层 IO 库常用 checked exception 强制处理外部失败。
业务服务通常使用 unchecked business exception，让 ControllerAdvice 统一转换，并让事务默认回滚。

### 能不能 catch Throwable？

通常不要。catch `Throwable` 会捕获 `Error`，可能掩盖严重 JVM 问题。
框架最外层可以做兜底日志，但业务代码不应该随意 catch。

## 回答评分点

高分答案应该覆盖：

- Throwable、Error、Exception、RuntimeException 结构。
- checked 和 unchecked 区别。
- 能结合业务异常、系统异常、下游异常。
- 能说明统一错误码和异常处理。
- 能指出不要随意捕获 Error/Throwable。

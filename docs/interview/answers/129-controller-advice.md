# 129 @ControllerAdvice 如何做统一异常处理？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`@ControllerAdvice` 可以集中处理 Controller 层抛出的异常，配合 `@ExceptionHandler` 把业务异常、
参数异常和系统异常转换成统一响应体和 HTTP 状态码。它能避免每个 Controller 重复 try-catch。

生产中要区分业务错误和系统错误，不能把堆栈直接返回给前端。

## 基本写法

示例：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusiness(BusinessException ex) {
        return ApiResponse.fail(ex.code(), ex.getMessage());
    }
}
```

`@RestControllerAdvice` 等于 `@ControllerAdvice` 加 `@ResponseBody`。

## 应该处理哪些异常？

常见类型：

- 业务异常。
- 参数校验异常。
- 类型转换异常。
- 认证授权异常。
- 限流异常。
- 下游超时异常。
- 未知系统异常。

不同异常要映射不同错误码和 HTTP 状态。

## 参数校验异常

Bean Validation 失败时，常见异常：

- `MethodArgumentNotValidException`。
- `ConstraintViolationException`。

应该返回清晰字段错误，但不要泄漏内部类名和堆栈。

## 系统异常

未知异常要：

- 记录完整日志。
- 返回通用错误码。
- 带 trace ID。
- 不返回堆栈。
- 触发告警或指标。

前端只需要知道请求失败和错误码，排查用 trace ID 关联日志。

## 错误响应体

统一响应通常包含：

- code。
- message。
- traceId。
- timestamp。
- details。

`details` 要谨慎，避免泄漏敏感信息。

## 在 eMall 项目中怎么讲？

订单接口中，库存不足是业务异常，可以返回明确业务错误码。

数据库连接失败是系统异常，应记录堆栈并返回系统繁忙。

支付下游超时可返回可重试错误，并带 trace ID 方便排查。

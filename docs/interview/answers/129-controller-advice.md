# 129 @ControllerAdvice 如何做统一异常处理？

[返回按分类学习面试题](../README.md)

## 核心结论

`@ControllerAdvice` 可以集中处理 Controller 层抛出的异常，配合 `@ExceptionHandler` 把业务异常、
参数异常和系统异常转换成统一响应体和 HTTP 状态码。它能避免每个 Controller 重复 try-catch。

生产中要区分业务错误和系统错误，不能把堆栈直接返回给前端。

## 处理链路

Controller 和应用服务只抛出语义明确的异常，`@RestControllerAdvice` 集中选择对应的
`@ExceptionHandler`，再映射 HTTP 状态、稳定错误码和安全消息。`@RestControllerAdvice` 等于
`@ControllerAdvice` 加 `@ResponseBody`，完整代码见[手写统一异常处理](653-exception-handler.md)。

## 参数校验异常

Bean Validation 失败时，常见异常：

- `MethodArgumentNotValidException`。
- `ConstraintViolationException`。

应该返回清晰字段错误，但不要泄漏内部类名和堆栈。

## 错误响应体

统一响应通常包含：

- code。
- message。
- traceId。
- timestamp。
- details。

`details` 要谨慎，避免泄漏敏感信息。

# 653 手写统一异常处理。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(new ApiError(ex.code(), ex.getMessage()));
    }
}
```

### 必测用例

- 业务异常、参数校验异常、资源不存在和未知异常映射到正确 HTTP 状态与错误码。
- 客户端响应不得包含堆栈、SQL 或内部地址，未知异常仍记录完整服务端诊断。
- 验证 trace ID、字段级校验信息和内容类型。

### 生产化差异

- 使用统一 `@RestControllerAdvice`，但错误码语义由领域拥有，不能把所有错误都返回 200。
- 日志需去重并脱敏，国际化消息与可告警的稳定错误码分离。

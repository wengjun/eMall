# 021 checked exception 和 unchecked exception 如何取舍？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

checked exception 会强制调用方在编译期处理，适合底层 API 明确要求调用方处理的外部资源失败，
例如文件、网络、IO。unchecked exception 不强制在签名中声明，更适合业务系统中的业务异常、
参数错误、状态冲突和不可恢复系统错误。

在 Spring 后端服务里，我通常用 unchecked 的 `BusinessException` 表达业务异常，
由统一异常处理转换成错误码和 HTTP 响应。这样 Service 方法签名保持业务语义，
事务默认也会对 unchecked exception 回滚。

## 从零基础理解

checked exception：

```java
public String readFile(String path) throws IOException {
    return Files.readString(Path.of(path));
}
```

调用方必须处理：

```java
try {
    readFile(path);
} catch (IOException ex) {
    // Handle error.
}
```

unchecked exception：

```java
throw new OrderStateException(orderId, currentStatus);
```

方法签名不强制写 `throws`，由上层统一处理。

## checked exception 的优点和缺点

优点：

- 编译器强制调用方意识到失败。
- 适合外部资源失败是 API 契约一部分的场景。
- 对底层库调用者更明确。

缺点：

- 多层业务代码中容易机械传递 `throws`。
- 方法签名被技术异常污染。
- Lambda 和 Stream 中处理较麻烦。
- 调用方经常只是包装再抛，实际价值有限。

## unchecked exception 的优点和缺点

优点：

- 业务方法签名更清晰。
- 可以由 `@ControllerAdvice` 统一转换响应。
- Spring 事务默认遇到 unchecked exception 回滚。
- 更适合领域规则冲突。

缺点：

- 编译器不强制处理。
- 如果没有统一异常治理，容易到处抛、到处 catch。
- 需要依赖测试和代码评审保证关键分支被处理。

## 在业务系统中怎么取舍？

### 底层基础设施可以保留 checked

例如文件导入、IO 读取、第三方 SDK 底层 API。

### 业务层转成明确业务异常

不要把 `SQLException`、`IOException` 一路抛到 Controller。

可以在基础设施层转换：

```java
try {
    jdbcTemplate.update(sql, args);
} catch (DataAccessException ex) {
    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "database operation failed", ex);
}
```

真实项目中系统异常可能用单独的 `SystemException` 或直接让框架处理，
关键是不要把底层异常原样暴露给用户。

## 和事务的关系

Spring 默认只对 unchecked exception 和 `Error` 回滚。
如果 checked exception 也要回滚，需要配置：

```java
@Transactional(rollbackFor = Exception.class)
```

所以在业务服务中使用 unchecked `BusinessException` 更常见。

## 在 eMall 项目中怎么讲？

例如订单状态不允许支付：

```java
throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid from " + order.status());
```

这是业务异常，用 unchecked 更合适。

库存服务超时、支付渠道失败，可以转换成下游不可用错误码，再由补偿、熔断、告警处理。

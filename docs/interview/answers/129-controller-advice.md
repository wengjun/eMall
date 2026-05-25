# 129 @ControllerAdvice 如何做统一异常处理？

[返回按分类学习面试题](../README.md)

## 题目

`@ControllerAdvice` 如何做统一异常处理？

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

## 深度增强：Spring 服务治理图

![Spring 微服务调用栈和治理边界](../assets/spring-service-stack.svg)

Spring 题要从框架机制讲到业务边界。Controller 负责协议适配，Service 负责业务事务，Repository 负责数据访问；
事务、AOP、校验、错误码、配置和观测都是为了让微服务在复杂调用中保持稳定。

## 深度增强：Java 17 分层示例

```java
record CreateOrderCommand(long userId, long skuId, int quantity) {
}

record CreateOrderResult(long orderId, String status) {
}

interface OrderApplicationService {
    CreateOrderResult create(CreateOrderCommand command);
}

final class OrderControllerAdapter {
    private final OrderApplicationService service;

    OrderControllerAdapter(OrderApplicationService service) {
        this.service = service;
    }

    CreateOrderResult submit(CreateOrderCommand command) {
        return service.create(command);
    }
}
```

这个示例表达分层边界：接口层不堆业务逻辑，业务层不依赖 Web 协议，命令和结果对象形成稳定契约。

## 深度增强：生产边界

框架默认值不能替代设计。事务传播、异常回滚、异步线程池、连接池、序列化、超时和重试都要显式治理。
尤其在订单、支付、库存链路中，要避免长事务、隐式重试和跨服务事务误用。

## 深度增强：面试高分表达

我会先解释框架原理，再说明在电商系统里怎么落地。高分回答要能把自动配置、AOP、事务、MVC、WebFlux、
校验和错误处理，连接到可维护性、可观测性、稳定性和故障恢复。

## 专家级完整回答

```text
@ControllerAdvice 配合 @ExceptionHandler 可以集中把 Controller 层异常转换成统一错误响应。
我会区分业务异常、参数异常、认证授权异常、下游异常和未知系统异常，分别映射错误码和 HTTP 状态。

未知异常只在服务端记录堆栈，客户端返回通用错误和 traceId，避免泄漏内部实现和敏感信息。
```

## 回答评分点

高分答案应该覆盖：

- `@ControllerAdvice` 和 `@ExceptionHandler`。
- 业务异常和系统异常区分。
- 参数校验异常处理。
- 统一错误码和 trace ID。
- 不返回堆栈给前端。

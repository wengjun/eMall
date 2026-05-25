# 157 错误码和 HTTP 状态码如何配合？

[返回按分类学习面试题](../README.md)

## 题目

错误码和 HTTP 状态码如何配合？

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

## 在 eMall 项目中怎么讲？

库存不足可以视为业务冲突，返回 409 和 `INVENTORY_NOT_ENOUGH`。

参数 quantity 小于 1 返回 400 和 `VALIDATION_FAILED`。

数据库异常返回 500 和 `SYSTEM_ERROR`，并带 traceId。

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
HTTP 状态码表达协议层和通用错误类别，业务错误码表达具体业务原因。两者应该配合使用：
400 表示参数错误，401/403 表示认证授权，404 表示资源不存在，409 表示业务状态冲突，429 表示限流，
500 表示系统异常；业务 code 进一步说明库存不足、优惠券过期、订单不可取消等原因。

关键是规范稳定，调用方不要猜语义。
```

## 回答评分点

高分答案应该覆盖：

- HTTP 状态码和业务码职责不同。
- 常见状态码映射。
- 不建议所有错误都 200。
- 业务失败和系统异常区分。
- 规范要稳定。

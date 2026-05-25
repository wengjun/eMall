# 156 如何设计统一响应体？

[返回按分类学习面试题](../README.md)

## 题目

如何设计统一响应体？

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
统一响应体要包含 code、message、data、traceId、timestamp 和可选 details。HTTP 状态码表达协议结果，
业务 code 表达业务语义。参数错误可以返回字段级 details，系统异常只返回通用错误和 traceId，
堆栈只记录在服务端。

响应体一旦对外发布要保持兼容，错误码语义不能随意改变。
```

## 回答评分点

高分答案应该覆盖：

- code/message/data/traceId/timestamp。
- HTTP 状态码和业务 code 分工。
- 参数错误 details。
- 不返回堆栈。
- 响应体要兼容。

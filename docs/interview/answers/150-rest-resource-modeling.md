# 150 REST API 的资源建模原则是什么？

[返回按分类学习面试题](../README.md)

## 题目

REST API 的资源建模原则是什么？

## 先给面试官的短答案

REST API 应围绕资源建模，而不是围绕动作建模。资源用名词表示，HTTP 方法表达动作，URL 表达资源层级和关系。
设计时要关注资源边界、幂等性、状态码、分页过滤、版本兼容和错误响应。

好的 API 应该稳定、可理解、可演进。

## 资源用名词

推荐：

```text
/orders
/orders/{orderId}
/users/{userId}/addresses
```

避免：

```text
/createOrder
/deleteOrder
/queryOrder
```

动作由 HTTP method 表达。

## HTTP 方法表达语义

常见语义：

- `GET`：查询资源。
- `POST`：创建资源或提交非幂等动作。
- `PUT`：整体替换资源。
- `PATCH`：局部更新资源。
- `DELETE`：删除资源。

方法语义要和幂等性一致。

## 资源层级

层级要表达真实归属关系。

例如用户地址：

```text
/users/{userId}/addresses/{addressId}
```

但层级不要太深。

过深 URL 往往说明资源边界不清晰。

## 动作型业务如何设计？

有些业务不是简单 CRUD。

例如取消订单：

```text
POST /orders/{orderId}/cancellation
```

或者：

```text
POST /orders/{orderId}:cancel
```

团队要统一风格。

关键是让动作具备清晰业务资源或命令语义。

## 查询、分页和过滤

列表查询使用 query 参数：

```text
GET /orders?status=PAID&page=1&pageSize=20
```

分页要限制最大 page size，避免大查询拖垮系统。

## 在 eMall 项目中怎么讲？

订单资源：

```text
GET /orders/{orderId}
POST /orders
POST /orders/{orderId}/cancellation
GET /users/{userId}/orders
```

库存扣减不一定暴露成简单 REST 资源给前端，内部可设计成命令接口并保证幂等。

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
REST 建模应围绕资源而不是动作。URL 用名词表达资源，HTTP 方法表达操作语义，状态码和响应体表达结果。
资源层级要清晰但不过深，列表查询用 query 参数并限制分页。复杂业务动作可以建模成子资源或命令资源。

对电商系统，我会特别关注幂等性、版本兼容、错误码、分页上限和资源边界，避免把 API 设计成一堆 RPC 动词。
```

## 回答评分点

高分答案应该覆盖：

- 资源用名词。
- HTTP 方法表达动作。
- URL 层级不过深。
- 复杂动作可建模为子资源。
- 关注幂等、分页、错误和版本。

# 151 GET、POST、PUT、PATCH、DELETE 应该如何使用？

[返回按分类学习面试题](../README.md)

## 题目

`GET`、`POST`、`PUT`、`PATCH`、`DELETE` 应该如何使用？

## 先给面试官的短答案

`GET` 用于查询资源，应该安全且幂等；`POST` 常用于创建资源或提交非幂等命令；
`PUT` 用于整体替换资源，通常幂等；`PATCH` 用于局部更新资源；`DELETE` 用于删除资源，语义上通常幂等。
方法选择要匹配资源语义、幂等性和副作用。

## GET

`GET` 用于读取资源。

特点：

- 不应产生业务副作用。
- 可以被缓存。
- 可以被浏览器或代理预取。
- 语义上幂等。

不能用 `GET /orders/create` 创建订单。

## POST

`POST` 常用于创建资源或提交命令。

例如：

```text
POST /orders
POST /orders/{orderId}/payment
```

`POST` 默认不保证幂等。

如果创建订单需要防重复，应额外设计幂等键。

## PUT

`PUT` 表示整体替换资源。

例如：

```text
PUT /users/{userId}/profile
```

客户端提交完整资源表示，重复提交同样内容结果相同，因此通常幂等。

## PATCH

`PATCH` 表示局部更新。

例如：

```text
PATCH /users/{userId}/profile
```

只更新提交字段。

要注意字段缺失和设置为空的语义差异。

## DELETE

`DELETE` 表示删除资源。

例如：

```text
DELETE /cart/items/{itemId}
```

重复删除同一资源，最终结果都是资源不存在，因此通常视为幂等。

## 在 eMall 项目中怎么讲？

订单查询用 `GET /orders/{orderId}`。

创建订单用 `POST /orders`，并通过幂等键防重复。

更新收货地址可以用 `PUT` 或 `PATCH`，取决于是整体替换还是局部更新。

取消订单可以建模为 `POST /orders/{orderId}/cancellation`。

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
HTTP 方法要匹配语义：GET 查询且无副作用，POST 创建或提交命令，PUT 整体替换且通常幂等，
PATCH 局部更新，DELETE 删除且通常幂等。不能把所有接口都做成 POST，也不能用 GET 做有副作用操作。

对电商系统，创建订单这类 POST 要额外设计幂等键，取消订单这类动作可以建模成子资源或命令资源。
```

## 回答评分点

高分答案应该覆盖：

- `GET` 安全且幂等。
- `POST` 常用于创建或命令。
- `PUT` 整体替换。
- `PATCH` 局部更新。
- `DELETE` 删除且通常幂等。

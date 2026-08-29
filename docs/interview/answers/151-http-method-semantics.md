# 151 GET、POST、PUT、PATCH、DELETE 应该如何使用？

[返回按分类学习面试题](../README.md)

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

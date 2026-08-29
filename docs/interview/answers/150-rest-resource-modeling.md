# 150 REST API 的资源建模原则是什么？

[返回按分类学习面试题](../README.md)

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

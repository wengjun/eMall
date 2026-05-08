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

## 深度完善：面向 L6 的回答框架

围绕「GET、POST、PUT、PATCH、DELETE 应该如何使用？」，高分答案不能停在概念定义，而要把「REST 契约、幂等、版本、错误码、签名、安全和客户端兼容」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「API 设计和网关治理」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `gateway、openapi、identity、risk、order 的外部 API 和内部服务 API` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：接口错误率、幂等冲突率、签名失败率、限流命中率、兼容性测试结果。
- 验证证据：OpenAPI 文档、契约测试、审计日志、网关指标、错误码看板和灰度记录。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：`GET`、`POST`、`PUT`、`PATCH`、`DELETE` 应该如何使用？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

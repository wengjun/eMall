# 017 如何避免所有业务逻辑堆在 Controller？

[返回按分类学习面试题](../README.md)

## 题目

如何避免所有业务逻辑堆在 Controller？

## 先给面试官的短答案

Controller 应该是协议适配层，只负责接收请求、参数校验、调用 Service、组装响应。
业务规则、事务边界、状态变化、下游调用、补偿和事件发布应该放在 Service、Domain、
Repository、Client、Messaging 等层里。

面试里可以这样说：

```text
我会保持 Controller 薄，让它只处理 HTTP 协议相关逻辑。
核心业务放到 Service，状态规则放到领域对象，数据访问放到 Repository，
下游调用放到 Client，异常响应由 ControllerAdvice 统一处理。
这样业务能力可以被 HTTP、MQ、定时任务和测试复用。
```

## Controller 应该做什么？

Controller 的职责：

- 接收 HTTP 请求。
- 绑定请求参数。
- 做基础参数校验。
- 调用应用服务。
- 把领域对象转换成响应 DTO。
- 返回 HTTP 状态码和响应体。

示例：

```java
@PostMapping("/api/orders")
public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
    Order order = orderService.create(
            request.requestId(),
            request.userId(),
            request.skuId(),
            request.quantity());
    return ApiResponse.success(OrderResponse.from(order));
}
```

这段 Controller 很薄，核心逻辑在 `orderService.create`。

## Controller 不应该做什么？

不应该：

- 直接写 SQL。
- 直接操作事务。
- 直接修改订单状态。
- 直接调用多个下游并处理复杂失败。
- 直接写 Outbox。
- 直接处理补偿重试。
- 到处 try-catch 拼响应。

坏例子：

```java
@PostMapping("/api/orders")
public Object create(@RequestBody Map<String, Object> body) {
    // Validate user.
    // Query price.
    // Calculate promotion.
    // Reserve inventory.
    // Insert order SQL.
    // Insert outbox SQL.
    // Catch all exceptions.
}
```

这种代码难测试、难复用、难维护。

## 推荐分层

```text
Controller -> Service -> Domain
                     -> Repository
                     -> Client
                     -> Outbox/Messaging
```

### Service

负责业务流程和事务边界。

例如订单服务：

- 查价格。
- 算优惠。
- 预占库存。
- 保存订单。
- 写 Outbox。
- 进入补偿状态。

### Domain

负责对象自身规则。

- 订单能否支付。
- 支付能否退款。
- 库存能否预占。

### Repository

负责数据访问。

### Client

负责下游服务调用。

### ControllerAdvice

负责统一异常响应。

## 为什么这样设计？

### 可测试

Service 可以不通过 HTTP 直接单元测试。

### 可复用

同一个业务能力可以被：

- HTTP API 调用。
- MQ 消费者调用。
- 定时补偿任务调用。
- 内部运维接口调用。

### 事务清晰

事务通常放在 Service，而不是 Controller。

### 协议隔离

如果未来从 HTTP 改成 gRPC，业务层不用重写。

## 在 eMall 项目中怎么讲？

eMall 的订单创建应该这样分：

- `OrderController`：接收 `CreateOrderRequest`。
- `OrderService`：编排价格、营销、库存、订单和 Outbox。
- `Order`：控制订单状态变化。
- `OrderRepository`：保存订单。
- `InventoryClient`：调用库存服务。

专家级表达：

```text
Controller 薄不是为了形式，而是为了让业务流程有明确事务边界，
让核心能力可测试、可复用、可观测。大型系统里 Controller 胖会导致协议层和业务层强耦合。
```

## 回答评分点

高分答案应该覆盖：

- Controller 是协议适配层。
- Service 承载业务流程和事务。
- Domain 承载状态规则。
- Repository/Client 分别隔离数据和下游。
- 能说明可测试、可复用、可维护的价值。

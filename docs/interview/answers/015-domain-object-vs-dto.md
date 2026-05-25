# 015 领域对象和 DTO 为什么要分开？

[返回按分类学习面试题](../README.md)

## 题目

领域对象和 DTO 为什么要分开？

## 先给面试官的短答案

DTO 是系统边界上的数据传输对象，领域对象是内部业务模型。两者职责不同、变化原因不同，
所以应该分开。

DTO 关注 API 字段、参数校验、序列化和兼容性；领域对象关注业务不变量、状态迁移和内部规则。
如果混用，容易导致外部请求直接污染内部状态，或内部敏感字段泄露给前端。

## 从零基础理解

前端创建订单时可能只传：

```json
{
  "requestId": "req-1",
  "userId": 10001,
  "skuId": 20001,
  "quantity": 1
}
```

这是 DTO。

但系统内部订单对象可能包含：

- orderId。
- userId。
- skuId。
- quantity。
- unitPrice。
- discountAmount。
- payableAmount。
- priceVersion。
- inventoryReservationId。
- status。
- failureReason。
- createdAt。
- updatedAt。

这就是领域对象。

前端请求不应该直接变成内部订单，也不应该让前端传入 `status=PAID`。

## DTO 的职责

DTO 负责系统边界：

- 接收 HTTP 请求。
- 返回 HTTP 响应。
- 表达 MQ payload。
- 做基础参数校验。
- 适配 API 版本。
- 控制字段暴露。

示例：

```java
public record CreateOrderRequest(
        @NotBlank String requestId,
        @Positive long userId,
        @Positive long skuId,
        @Positive int quantity
) {
}
```

## 领域对象的职责

领域对象负责业务规则：

```java
public Order markPaid() {
    if (status != OrderStatus.CREATED) {
        throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid");
    }
    return new Order(..., OrderStatus.PAID, ...);
}
```

领域对象不应该关心 JSON 字段名，也不应该被前端 API 结构牵着走。

## 混用会有什么风险？

### 外部字段污染内部状态

如果直接用 `Order` 接收请求，用户可能传：

```json
{
  "orderId": 1,
  "status": "PAID",
  "payableAmount": 0
}
```

这会绕过业务规则。

### 内部字段泄露

订单对象可能包含：

- failureReason。
- internalNotes。
- audit fields。
- risk flags。
- cost fields。

这些不一定能返回给前端。

### API 变化影响领域模型

前端要新增展示字段，不应该导致内部领域对象乱加字段。

### 领域模型变化影响 API 兼容

内部重构字段名，不应该破坏外部 API。

## DTO 和领域对象如何转换？

Controller 调用 Service：

```java
Order order = orderService.create(
        request.requestId(),
        request.userId(),
        request.skuId(),
        request.quantity());
```

领域对象转响应：

```java
public record OrderResponse(long orderId, String status, BigDecimal payableAmount) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.orderId(), order.status().name(), order.payableAmount());
    }
}
```

简单项目可以手写转换。复杂项目可以用 MapStruct，但核心映射规则仍然要清楚。

## 在 eMall 项目中怎么讲？

下单 API 的请求 DTO 只包含用户输入。订单领域对象由服务端生成：

- 订单 ID 由服务端生成。
- 价格由价格服务返回。
- 优惠由营销服务计算。
- 库存预占 ID 由订单服务生成。
- 状态由订单服务控制。

这体现了清晰边界。

专家级表达：

```text
DTO 是边界契约，领域对象是业务模型。分开后，外部 API 可以演进，
内部领域规则也可以重构，二者不会互相污染。
在交易系统里这很重要，因为状态、金额、库存和敏感字段不能由外部请求直接控制。
```

## 回答评分点

高分答案应该覆盖：

- DTO 和领域对象职责不同。
- 能说明防止外部污染内部状态。
- 能说明防止敏感字段泄露。
- 能说明 API 和领域模型独立演进。
- 能结合下单请求和订单领域对象。

# 011 枚举适合表达哪些业务状态？

[返回按分类学习面试题](../README.md)

## 题目

枚举适合表达哪些业务状态？

## 先给面试官的短答案

枚举适合表达“取值有限、语义稳定、需要编译期约束”的业务状态或类型。
在电商系统里，订单状态、支付状态、库存预占状态、Outbox 事件状态、错误码都很适合用枚举。

面试里可以这样回答：

```text
我会用枚举表达有限且稳定的业务状态，例如订单 CREATED、PAID、CANCELLED，
支付 CREATED、SUCCEEDED、REFUNDED。枚举的价值是避免字符串拼写错误，
让编译器帮助约束合法值，并配合状态机限制非法流转。
但枚举一旦进入数据库、API 或 MQ，就变成对外契约，扩展时要考虑兼容性。
```

## 从零基础理解：为什么不用字符串？

如果用字符串表达订单状态：

```java
String status = "PAID";
```

任何人都可能写错：

```java
String status = "PAYED";
String status = "paid";
String status = "SUCCESS";
```

这些错误编译器发现不了，只有运行时才可能暴露。

枚举写法：

```java
public enum OrderStatus {
    CREATED,
    PENDING_RETRY,
    PAID,
    CANCELLED,
    CLOSED
}
```

使用时：

```java
if (order.status() == OrderStatus.PAID) {
    // Handle paid order.
}
```

这样状态值只能来自枚举定义。

## 适合用枚举的业务状态

### 订单状态

订单是电商核心状态机，适合枚举：

```java
public enum OrderStatus {
    CREATED,
    PENDING_RETRY,
    PAID,
    CANCELLED,
    CLOSED,
    FULFILLING,
    COMPLETED,
    AFTER_SALES
}
```

订单状态不只是展示字段，它决定后续动作：

- `CREATED` 可以支付或取消。
- `PAID` 可以履约，但不能直接取消。
- `PENDING_RETRY` 需要补偿任务处理。
- `COMPLETED` 可以进入售后。

### 支付状态

```java
public enum PaymentStatus {
    CREATED,
    SUCCEEDED,
    FAILED,
    REFUNDING,
    REFUNDED
}
```

支付状态会影响：

- 是否允许退款。
- 是否需要确认订单。
- 是否需要渠道对账。
- 是否允许重复回调幂等返回。

### 库存预占状态

```java
public enum ReservationStatus {
    RESERVED,
    CONFIRMED,
    RELEASED,
    REJECTED
}
```

库存状态要严格控制：

- `RESERVED` 可以确认或释放。
- `CONFIRMED` 不能再释放。
- `RELEASED` 不能再确认。
- `REJECTED` 表示预占失败。

### Outbox 事件状态

```java
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
```

Outbox 状态用于后台 Relay 扫描和重试。它不是简单展示字段，而是可靠消息发布流程的控制点。

### 错误码

错误码也适合枚举：

```java
public enum ErrorCode {
    BAD_REQUEST,
    NOT_FOUND,
    CONFLICT,
    TOO_MANY_REQUESTS,
    DOWNSTREAM_UNAVAILABLE,
    INTERNAL_ERROR
}
```

错误码需要稳定、可聚合、可监控。

## 枚举不应该滥用

不适合枚举的场景：

- 取值由用户或商家动态配置。
- 类型集合经常由第三方扩展。
- 需要复杂层级和不同字段结构。
- 值本质上是数据库配置表。

例如商品类目不适合写成枚举：

```java
public enum Category {
    PHONE,
    FOOD,
    CLOTHES
}
```

因为类目会频繁变更，还需要运营后台管理。

## 枚举和状态机的关系

枚举只定义合法状态，状态机定义合法流转。

错误做法：

```java
order.setStatus(OrderStatus.PAID);
```

任何地方都能改状态，枚举也保护不了业务。

更好的做法：

```java
public Order markPaid() {
    if (status != OrderStatus.CREATED) {
        throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid");
    }
    return new Order(..., OrderStatus.PAID, ...);
}
```

枚举要和领域方法、数据库约束、事件、审计一起工作。

## 在 eMall 项目中怎么讲？

eMall 中可以重点讲：

- `OrderStatus` 控制订单创建、支付、取消、补偿。
- `PaymentStatus` 控制支付成功、退款和对账。
- `ReservationStatus` 控制库存预占、确认、释放。
- `OutboxStatus` 控制可靠事件发布。
- `ErrorCode` 控制统一响应和监控聚合。

专家级表达：

```text
枚举是业务状态建模的第一步，但不是完整状态机。
我会用枚举限制合法状态值，再用领域方法限制合法状态迁移，
并通过数据库、Outbox、审计和测试保证状态变化可追踪、可恢复。
```

## 常见追问

### 枚举入库保存 name 还是 code？

保存 `name` 可读性好，但重命名风险大。保存稳定 code 兼容性更好，但需要额外映射。

生产建议：

- 对外契约或数据库里的值不要随便改。
- 如果状态可能改名，使用稳定 code。
- 改枚举要走兼容发布流程。

### 枚举可以有字段和方法吗？

可以。

```java
public enum OrderStatus {
    CREATED(true),
    PAID(false),
    CANCELLED(false);

    private final boolean payable;

    OrderStatus(boolean payable) {
        this.payable = payable;
    }

    public boolean payable() {
        return payable;
    }
}
```

但复杂业务规则不要全部塞进枚举，避免枚举变成大杂烩。

## 回答评分点

高分答案应该覆盖：

- 枚举适合有限、稳定、语义明确的状态。
- 能举订单、支付、库存、Outbox、错误码例子。
- 能区分枚举和值动态配置。
- 能说明枚举不是完整状态机。
- 能意识到数据库、API、MQ 中枚举的兼容性风险。

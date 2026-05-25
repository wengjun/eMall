# 016 贫血模型和充血模型各有什么优缺点？

[返回按分类学习面试题](../README.md)

## 题目

贫血模型和充血模型各有什么优缺点？

## 先给面试官的短答案

贫血模型是对象主要只有字段和 getter/setter，业务逻辑集中在 Service。
充血模型是对象不仅有数据，也包含和自身状态相关的业务行为。

贫血模型简单、容易上手，适合 CRUD；缺点是业务规则容易散落。
充血模型能保护业务不变量，适合复杂领域；缺点是设计门槛更高，和 ORM 配合要注意。

我的实践是：核心交易领域适度充血，简单后台配置可以贫血。

## 从零基础理解

贫血模型：

```java
public class Order {
    private OrderStatus status;

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
```

业务逻辑在 Service：

```java
if (order.getStatus() == OrderStatus.CREATED) {
    order.setStatus(OrderStatus.PAID);
}
```

充血模型：

```java
public Order markPaid() {
    if (status != OrderStatus.CREATED) {
        throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid");
    }
    return new Order(..., OrderStatus.PAID, ...);
}
```

订单自己知道如何合法变成已支付。

## 贫血模型优点

- 简单。
- 新人容易理解。
- 和数据库表映射直观。
- 适合后台管理和 CRUD。
- Service 编排清晰。

例如类目管理、品牌管理、简单配置表，用贫血模型通常够用。

## 贫血模型缺点

- 业务规则散落在多个 Service。
- 对象无法保护自身不变量。
- 很多地方都能 set 状态。
- 复杂后容易重复校验。
- 测试必须绕 Service，领域规则不独立。

订单、库存、支付如果完全贫血，很容易出现非法状态。

## 充血模型优点

- 业务规则靠近数据。
- 状态迁移更清晰。
- 更容易保护不变量。
- 领域方法可单元测试。
- 代码表达更贴近业务语言。

例如：

```java
inventory.reserve(quantity);
order.markPaid();
payment.refunded();
```

这些方法名本身就是业务动作。

## 充血模型缺点

- 设计门槛高。
- 过度设计会复杂。
- 和某些 ORM 代理、无参构造、懒加载配合要注意。
- 容易把基础设施逻辑错误塞进领域对象。

领域对象不应该直接发 MQ、调 HTTP、操作数据库。它应该处理自身规则，流程编排仍然在 Service。

## 推荐实践：适度充血

核心交易对象适合适度充血：

- `Order.markPaid()`
- `Order.markCancelled()`
- `InventoryItem.reserve()`
- `InventoryReservation.confirm()`
- `PaymentOrder.succeed()`

Service 负责：

- 开事务。
- 调 Repository。
- 调下游 Client。
- 写 Outbox。
- 触发补偿。

Domain 负责：

- 状态是否合法。
- 金额是否合法。
- 对象自身不变量。

## 在 eMall 项目中怎么讲？

eMall 中可以这样描述：

```text
OrderService 负责编排价格、营销、库存、订单保存和 Outbox；
Order 对象负责表达订单状态变化，例如 markPaid、markCancelled。
这样既避免 Controller/Service 过胖，也不让领域对象依赖数据库和 MQ。
```

这就是适度充血。

## 常见追问

### 充血模型是不是 DDD？

它是 DDD 中常见实践之一，但不是用了充血模型就等于 DDD。
DDD 还包括限界上下文、聚合、领域事件、领域服务、上下文映射等。

### 所有系统都要充血模型吗？

不是。简单 CRUD 用贫血模型更高效。复杂核心领域才值得投入建模成本。

## 回答评分点

高分答案应该覆盖：

- 能定义贫血和充血。
- 能说出各自优缺点。
- 能说明核心交易适度充血。
- 能区分 Domain 和 Service 职责。
- 能避免“所有地方都 DDD”的过度设计。

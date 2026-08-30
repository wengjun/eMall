# 232 下单后库存预占失败怎么办？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

下单后库存预占失败时，订单不能继续进入待支付状态，应进入创建失败、已取消或库存不足状态。
如果订单已创建，要通过状态机和补偿释放相关资源，并给用户明确提示。

库存预占是交易能否继续的关键决策点，不能忽略失败。

## 常见流程

安全流程：

```text
创建订单草稿
-> 请求库存预占
-> 预占成功：订单变为待支付
-> 预占失败：订单变为取消或失败
```

订单状态必须体现库存结果。

## 失败原因

可能原因：

- 库存不足。
- SKU 已下架。
- 仓库不可用。
- 库存服务超时。
- 并发竞争失败。

业务失败和系统失败要区分。库存不足不应反复重试，服务超时可以谨慎重试或查询确认。

## 补偿处理

如果订单已占用优惠券、积分或锁定活动资格，库存预占失败后要释放这些资源。

补偿必须幂等，避免重复释放或释放他人资源。

## 电商系统实践

用户提交订单后，如果库存服务返回库存不足，订单服务把订单标记为 `CANCELED_OUT_OF_STOCK`，
释放优惠锁定，并提示用户商品库存不足。

## 深度增强：一致性闭环图

![对账和补偿的一致性闭环](../assets/consistency-compensation-loop.svg)

库存预占失败不是一个简单的异常返回，它会影响订单状态、优惠锁定、活动资格和用户体验。
如果订单已经创建，就必须通过状态机把订单推进到可解释、可恢复、可对账的终态。

## 深度增强：Java 17 状态推进

订单状态机要显式表达库存失败：

```java
public enum OrderStatus {
    DRAFT,
    PENDING_PAYMENT,
    CANCELED_OUT_OF_STOCK,
    CANCELED_SYSTEM_FAILURE
}

public final class Order {

    private OrderStatus status;
    private String failureReason;

    public void markInventoryReserved() {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft order can reserve inventory.");
        }
        status = OrderStatus.PENDING_PAYMENT;
    }

    public void cancelBecauseOutOfStock(String reason) {
        if (status == OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Paid path must not be canceled as out of stock.");
        }
        status = OrderStatus.CANCELED_OUT_OF_STOCK;
        failureReason = reason;
    }
}
```

应用服务要区分业务失败和系统失败：

```java
@Transactional
public OrderResult createOrder(CreateOrderCommand command) {
    Order order = orderRepository.save(Order.draft(command));
    InventoryReserveResult result = inventoryClient.reserve(order.id(), command.items());

    if (result == InventoryReserveResult.OUT_OF_STOCK) {
        order.cancelBecauseOutOfStock("Inventory is not enough.");
        couponClient.release(command.couponHoldId());
        return OrderResult.rejected(order.id(), "OUT_OF_STOCK");
    }

    if (result == InventoryReserveResult.TIMEOUT) {
        order.markSystemFailure("Inventory reservation timeout.");
        compensationRepository.save(CompensationTask.confirmInventory(order.id()));
        return OrderResult.processing(order.id());
    }

    order.markInventoryReserved();
    return OrderResult.pendingPayment(order.id());
}
```

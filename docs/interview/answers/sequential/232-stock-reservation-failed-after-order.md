# 232 下单后库存预占失败怎么办？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

下单后库存预占失败怎么办？

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

## 在 eMall 项目中怎么讲？

用户提交订单后，如果库存服务返回库存不足，订单服务把订单标记为 `CANCELED_OUT_OF_STOCK`，
释放优惠锁定，并提示用户商品库存不足。

## 深度增强：一致性闭环图

![对账和补偿的一致性闭环](../../assets/consistency-compensation-loop.svg)

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

## 深度增强：面试高分表达

```text
库存不足是业务失败，不应该反复重试；库存服务超时是系统不确定状态，不能直接当作失败或成功。
我会让订单状态机显式记录库存结果，库存不足时取消订单并释放优惠，超时时进入处理中并由补偿任务查询确认。
核心是用户能看到明确状态，系统能通过补偿和对账收敛。
```

## 专家级完整回答

```text
库存预占失败说明交易不能继续。订单应进入失败或取消状态，不能进入待支付。
业务上要区分库存不足和系统超时，库存不足直接提示用户，系统超时可以查询确认或短暂重试。

如果订单创建过程中还锁定了优惠券、积分或活动资格，失败后要通过幂等补偿释放这些资源。
```

## 回答评分点

高分答案应该覆盖：

- 预占失败不能继续待支付。
- 订单状态机要表达失败原因。
- 业务失败和系统失败要区分。
- 已锁定资源要补偿释放。
- 补偿要幂等。
## 深度完善：专项验收清单

围绕「下单后库存预占失败怎么办？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
回答时要把概念、代码、数据、失败路径和指标串起来，证明自己不是只理解单点知识。

### 项目落点

- 先说明它在 eMall 哪个模块或链路中出现，例如交易、库存、支付、搜索、风控、发布或可观测性。
- 再说明它保护的核心目标：正确性、可用性、延迟、成本、安全或协作效率。
- 最后补失败场景：超时、重试、重复请求、状态不一致、热点流量、配置错误或发布回滚。

### 验收证据

- 代码证据：关键类、状态机、唯一约束、事务边界、线程池隔离或配置项。
- 测试证据：单元测试、集成测试、契约测试、压测、故障注入或回归用例。
- 运行证据：指标看板、Trace、结构化日志、告警、Runbook、对账结果或补偿记录。

### 高分收束

面试最后要回到取舍：当前方案为什么足够简单可靠，什么时候需要升级，升级时如何灰度、回滚和验证。
这样回答能体现生产系统判断力，而不是只罗列技术名词。

深度完善标记：专题增强答案已补项目落点、验收证据和取舍收束。

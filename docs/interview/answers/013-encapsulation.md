# 013 面向对象中的封装在业务系统里具体体现在哪里？

[返回按分类学习面试题](../README.md)

## 题目

面向对象中的封装在业务系统里具体体现在哪里？

## 先给面试官的短答案

封装不是简单把字段设成 private，而是把业务规则和数据修改入口收拢到正确的位置，
防止外部代码随意破坏业务不变量。

在电商系统里，封装体现在：

- 订单状态不能被任意 set，只能通过 `markPaid`、`markCancelled` 等业务方法变化。
- 库存数量不能被任意修改，只能通过预占、确认、释放流程变化。
- 支付流水不能覆盖更新，只能追加写入。
- 敏感字段加密和脱敏不能散落在业务代码里。

## 从零基础理解：什么是封装？

初学者通常理解封装是：

```java
private String name;

public String getName() {
    return name;
}

public void setName(String name) {
    this.name = name;
}
```

但这只是语法层面的封装。如果给所有字段都生成 setter，外部仍然可以随便改对象状态。

真正的封装是：

```text
对象不暴露随意修改内部状态的入口，而是暴露有业务含义的方法。
```

## 订单状态封装

不好的写法：

```java
order.setStatus(OrderStatus.PAID);
```

问题是任何代码都能把订单改成已支付，即使订单已经取消。

更好的写法：

```java
public Order markPaid() {
    if (status != OrderStatus.CREATED) {
        throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid from " + status);
    }
    return new Order(..., OrderStatus.PAID, ...);
}
```

这样状态变化有明确入口，也能保护规则。

## 库存数量封装

库存有三个重要数量：

- 可售库存。
- 已预占库存。
- 已售库存。

如果外部能随便 set：

```java
inventory.setAvailable(100);
inventory.setReserved(-1);
```

系统很容易出现库存为负、已售大于总库存等问题。

更好的方式：

```java
public InventoryItem reserve(int quantity) {
    if (available < quantity) {
        throw new BusinessException(ErrorCode.CONFLICT, "insufficient stock");
    }
    return new InventoryItem(skuId, available - quantity, reserved + quantity, sold, Instant.now());
}
```

封装的目标是保护库存不变量：

```text
available >= 0
reserved >= 0
sold >= 0
```

## 支付流水封装

支付流水是审计数据。它应该追加写，不应该随意覆盖。

错误思路：

```java
ledger.setAmount(newAmount);
ledger.setDirection(DEBIT);
```

正确思路：

```java
paymentLedgerRepository.save(new PaymentLedgerEntry(...));
```

支付成功写 CREDIT，退款写 DEBIT。历史流水保留，便于对账和审计。

## 敏感数据封装

手机号加密不应该散落在 Controller 或 Service 的各个角落。

更好的方式是封装到 `FieldEncryptor`：

```java
String encryptedMobile = fieldEncryptor.encrypt(user.mobile());
String mobileHash = fieldEncryptor.lookupHash(user.mobile());
```

这样业务代码不用知道 AES-GCM、HMAC、IV 等细节。

## 封装和分布式服务边界

封装不只存在于类内部，也存在于服务边界。

订单服务拥有订单状态，其他服务不能直接改订单表。
库存服务拥有库存数量，订单服务只能调用库存 API。
支付服务拥有支付流水，订单服务不能直接写支付表。

这就是系统级封装。

专家级表达：

```text
类级封装保护对象不变量，服务级封装保护数据所有权。
在微服务系统里，封装不仅是 private 字段，也是服务边界和数据库所有权。
```

## 常见追问

### 有 getter/setter 就是封装吗？

不是。只有 getter/setter 只是隐藏字段访问语法，没有保护业务规则。
如果 setter 可以任意修改状态，业务不变量仍然会被破坏。

### 封装会不会让代码更复杂？

短期看会多一些方法，长期看能减少状态被随意修改导致的 bug。
核心业务越复杂，越需要封装。

## 回答评分点

高分答案应该覆盖：

- 能超越 getter/setter 解释封装。
- 能讲业务不变量。
- 能举订单、库存、支付流水例子。
- 能扩展到服务边界和数据所有权。
- 能说明封装对可维护性和安全性的价值。

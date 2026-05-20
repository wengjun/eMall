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

## 深度增强：工程化理解图

![Java 工程能力从语法到生产设计](../assets/java-engineering-model.svg)

这类题不能只停留在语法解释。生产系统更关心它如何改善建模、降低误用、保护兼容性、提升可测试性，
以及能否让团队在多人协作中保持稳定边界。回答时要从语言特性落到业务约束和工程治理。

## 深度增强：Java 17 落地示例

```java
import java.util.Objects;

record StableApiField(String name, String type, boolean required) {

    StableApiField {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        if (name.isBlank() || type.isBlank()) {
            throw new IllegalArgumentException("API field metadata must be explicit");
        }
    }
}

final class ApiCompatibilityPolicy {

    boolean canAddField(StableApiField field) {
        return !field.required();
    }
}
```

这段代码体现 Java 17 在工程建模中的价值：用 `record` 表达不可变数据，用构造校验保护边界，
用小的策略类表达兼容规则。面试中要把语法能力和 API 演进、错误预防、团队协作联系起来。

## 深度增强：生产边界

语言特性不是越新越好。核心原则是可读、可测、可维护、可兼容。任何语法选择都要能让代码意图更清晰，
而不是为了炫技。公共 API、金额、时间、状态、异常和 DTO 都要有稳定约束，避免线上数据被随意破坏。

## 深度增强：面试高分表达

我会先回答概念，再说明它在电商系统中的真实作用。例如金额要避免精度错误，状态要可兼容扩展，
DTO 和领域对象要隔离外部契约和内部模型。这样能体现我不是只会写 Java 语法，而是能做工程设计。

## 回答评分点

高分答案应该覆盖：

- 能超越 getter/setter 解释封装。
- 能讲业务不变量。
- 能举订单、库存、支付流水例子。
- 能扩展到服务边界和数据所有权。
- 能说明封装对可维护性和安全性的价值。

## 深度完善：面向 L6 的回答框架

围绕「面向对象中的封装在业务系统里具体体现在哪里？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「Java 语言和工程基础」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `common、order、inventory、payment 的 DTO、值对象、异常和公共 API` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：代码评审问题数、缺陷逃逸率、兼容性测试结果、静态检查违规数。
- 验证证据：代码规范、单元测试、契约测试、兼容性用例和重构前后缺陷数据。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引
本题复习重点：面向对象中的封装在业务系统里具体体现在哪里？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

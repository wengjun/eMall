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

- 枚举适合有限、稳定、语义明确的状态。
- 能举订单、支付、库存、Outbox、错误码例子。
- 能区分枚举和值动态配置。
- 能说明枚举不是完整状态机。
- 能意识到数据库、API、MQ 中枚举的兼容性风险。

## 深度完善：面向 L6 的回答框架

围绕「枚举适合表达哪些业务状态？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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
本题复习重点：枚举适合表达哪些业务状态？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

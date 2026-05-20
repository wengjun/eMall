# 004 `switch` 表达式相比传统 `switch` 有什么优势？

[返回按分类学习面试题](../README.md)

## 题目

`switch` 表达式相比传统 `switch` 有什么优势？

## 先给面试官的短答案

传统 `switch` 是语句，主要用于执行分支逻辑；新的 `switch` 表达式可以直接返回值，
语法更简洁，也减少忘记 `break` 导致的 fall-through 问题。

在业务系统里，它很适合表达有限状态的映射，例如订单状态到下一步动作、支付状态到展示文案、
库存预占状态到处理策略。

但我不会把复杂业务流程都塞进 `switch`。复杂状态机仍然需要领域方法、状态迁移校验、
审计、事件和补偿机制。

## 从零基础理解：传统 `switch` 的问题

传统写法：

```java
String action;
switch (order.status()) {
    case CREATED:
        action = "PAY";
        break;
    case PAID:
        action = "FULFILL";
        break;
    case CANCELLED:
    case CLOSED:
        action = "NONE";
        break;
    default:
        action = "RETRY";
        break;
}
```

问题：

- 代码啰嗦。
- 每个分支都要写 `break`。
- 忘记 `break` 会继续执行下一个分支。
- 变量要先声明，再在分支里赋值。
- 分支多时可读性下降。

新的 `switch` 表达式：

```java
String action = switch (order.status()) {
    case CREATED -> "PAY";
    case PAID -> "FULFILL";
    case CANCELLED, CLOSED -> "NONE";
    case PENDING_RETRY -> "RETRY";
};
```

它直接表达“根据状态得到动作”。

## 核心优势

### 减少 fall-through 错误

传统 `switch` 如果忘记 `break`，会继续执行后面的 case。
在订单、支付、库存状态处理中，这类错误可能导致严重业务问题。

`->` 写法默认不会 fall-through，更安全。

### 可以作为表达式返回值

`switch` 表达式可以直接赋值：

```java
HttpStatus httpStatus = switch (errorCode) {
    case NOT_FOUND -> HttpStatus.NOT_FOUND;
    case CONFLICT -> HttpStatus.CONFLICT;
    case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
    default -> HttpStatus.BAD_REQUEST;
};
```

这适合统一异常处理中错误码到 HTTP 状态码的映射。

### 分支更紧凑

多个状态可以写在同一分支：

```java
case CANCELLED, CLOSED -> "NONE";
```

比传统写法更清楚。

### 更适合枚举状态

电商系统有大量枚举：

- `OrderStatus`
- `PaymentStatus`
- `ReservationStatus`
- `OutboxStatus`
- `ErrorCode`

`switch` 表达式很适合这种有限集合。

## 在 eMall 项目中怎么用？

### 错误码映射

```java
HttpStatus status = switch (errorCode) {
    case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
    case FORBIDDEN -> HttpStatus.FORBIDDEN;
    case NOT_FOUND -> HttpStatus.NOT_FOUND;
    case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
    case DOWNSTREAM_UNAVAILABLE, SYSTEM_BUSY -> HttpStatus.SERVICE_UNAVAILABLE;
    case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    default -> HttpStatus.BAD_REQUEST;
};
```

这种映射逻辑短、稳定、清晰。

### 状态展示

```java
String display = switch (payment.status()) {
    case CREATED -> "Waiting for payment";
    case SUCCEEDED -> "Paid";
    case REFUNDING -> "Refunding";
    case REFUNDED -> "Refunded";
    case FAILED -> "Failed";
};
```

### 简单策略选择

```java
RetryPolicy policy = switch (event.status()) {
    case PENDING -> RetryPolicy.immediate();
    case FAILED -> RetryPolicy.exponentialBackoff();
    case PUBLISHED -> RetryPolicy.none();
};
```

## 什么时候不应该用？

### 复杂状态机

如果状态迁移包含：

- 权限校验。
- 数据库写入。
- 事件发布。
- 审计记录。
- 补偿状态。
- 下游调用。

就不应该只用一个大 `switch` 搞定。

例如订单支付不是简单：

```java
status = switch (status) {
    case CREATED -> PAID;
    default -> throw ...
};
```

真实逻辑还要确认库存、写订单、写 Outbox、处理失败补偿。

### 分支持续膨胀

如果 `switch` 有几十个 case，并且每个 case 都很多行，通常说明需要策略模式。

例如不同支付渠道的处理：

```text
ALIPAY -> ...
WECHAT -> ...
CARD -> ...
PAYPAL -> ...
```

这种更适合 `PaymentChannel` 接口加多个实现。

## 常见追问

### `switch` 表达式能不能抛异常？

可以。

```java
OrderAction action = switch (status) {
    case CREATED -> OrderAction.PAY;
    case PAID -> OrderAction.FULFILL;
    case CANCELLED -> OrderAction.NONE;
    default -> throw new BusinessException(ErrorCode.CONFLICT, "unsupported status");
};
```

### 它能不能替代策略模式？

不能完全替代。简单映射可以用 `switch`，复杂可扩展业务用策略模式。

判断标准：

- 分支稳定、逻辑短：用 `switch`。
- 分支经常扩展、逻辑复杂、需要独立测试：用策略模式。

### 专家级完整回答

```text
switch 表达式相比传统 switch 的优势是更安全、更简洁，可以直接返回值，
并且避免忘记 break 造成 fall-through。它很适合枚举状态映射，例如错误码到 HTTP 状态、
订单状态到展示动作、支付状态到处理策略。

但我不会把复杂业务流程都放进 switch。像订单支付、库存确认、支付回调这类流程，
涉及事务、状态机、Outbox、补偿和审计，应该由领域服务或应用服务承载。
switch 表达式适合表达简单决策，不适合替代完整业务建模。
```

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

- 能解释表达式和值返回。
- 能解释避免 fall-through。
- 能结合枚举状态和错误码映射。
- 能说明不适合复杂业务流程。
- 能说出和策略模式的边界。

## 深度完善：面向 L6 的回答框架

围绕「`switch` 表达式相比传统 `switch` 有什么优势？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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
本题复习重点：`switch` 表达式相比传统 `switch` 有什么优势？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

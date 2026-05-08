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

- DTO 和领域对象职责不同。
- 能说明防止外部污染内部状态。
- 能说明防止敏感字段泄露。
- 能说明 API 和领域模型独立演进。
- 能结合下单请求和订单领域对象。

## 深度完善：面向 L6 的回答框架

围绕「领域对象和 DTO 为什么要分开？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

重复补强内容已合并到 [面试补强共享框架](../deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：领域对象和 DTO 为什么要分开？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

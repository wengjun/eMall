# 017 如何避免所有业务逻辑堆在 Controller？

[返回按分类学习面试题](../README.md)

## 题目

如何避免所有业务逻辑堆在 Controller？

## 先给面试官的短答案

Controller 应该是协议适配层，只负责接收请求、参数校验、调用 Service、组装响应。
业务规则、事务边界、状态变化、下游调用、补偿和事件发布应该放在 Service、Domain、
Repository、Client、Messaging 等层里。

面试里可以这样说：

```text
我会保持 Controller 薄，让它只处理 HTTP 协议相关逻辑。
核心业务放到 Service，状态规则放到领域对象，数据访问放到 Repository，
下游调用放到 Client，异常响应由 ControllerAdvice 统一处理。
这样业务能力可以被 HTTP、MQ、定时任务和测试复用。
```

## Controller 应该做什么？

Controller 的职责：

- 接收 HTTP 请求。
- 绑定请求参数。
- 做基础参数校验。
- 调用应用服务。
- 把领域对象转换成响应 DTO。
- 返回 HTTP 状态码和响应体。

示例：

```java
@PostMapping("/api/orders")
public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
    Order order = orderService.create(
            request.requestId(),
            request.userId(),
            request.skuId(),
            request.quantity());
    return ApiResponse.success(OrderResponse.from(order));
}
```

这段 Controller 很薄，核心逻辑在 `orderService.create`。

## Controller 不应该做什么？

不应该：

- 直接写 SQL。
- 直接操作事务。
- 直接修改订单状态。
- 直接调用多个下游并处理复杂失败。
- 直接写 Outbox。
- 直接处理补偿重试。
- 到处 try-catch 拼响应。

坏例子：

```java
@PostMapping("/api/orders")
public Object create(@RequestBody Map<String, Object> body) {
    // Validate user.
    // Query price.
    // Calculate promotion.
    // Reserve inventory.
    // Insert order SQL.
    // Insert outbox SQL.
    // Catch all exceptions.
}
```

这种代码难测试、难复用、难维护。

## 推荐分层

```text
Controller -> Service -> Domain
                     -> Repository
                     -> Client
                     -> Outbox/Messaging
```

### Service

负责业务流程和事务边界。

例如订单服务：

- 查价格。
- 算优惠。
- 预占库存。
- 保存订单。
- 写 Outbox。
- 进入补偿状态。

### Domain

负责对象自身规则。

- 订单能否支付。
- 支付能否退款。
- 库存能否预占。

### Repository

负责数据访问。

### Client

负责下游服务调用。

### ControllerAdvice

负责统一异常响应。

## 为什么这样设计？

### 可测试

Service 可以不通过 HTTP 直接单元测试。

### 可复用

同一个业务能力可以被：

- HTTP API 调用。
- MQ 消费者调用。
- 定时补偿任务调用。
- 内部运维接口调用。

### 事务清晰

事务通常放在 Service，而不是 Controller。

### 协议隔离

如果未来从 HTTP 改成 gRPC，业务层不用重写。

## 在 eMall 项目中怎么讲？

eMall 的订单创建应该这样分：

- `OrderController`：接收 `CreateOrderRequest`。
- `OrderService`：编排价格、营销、库存、订单和 Outbox。
- `Order`：控制订单状态变化。
- `OrderRepository`：保存订单。
- `InventoryClient`：调用库存服务。

专家级表达：

```text
Controller 薄不是为了形式，而是为了让业务流程有明确事务边界，
让核心能力可测试、可复用、可观测。大型系统里 Controller 胖会导致协议层和业务层强耦合。
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

- Controller 是协议适配层。
- Service 承载业务流程和事务。
- Domain 承载状态规则。
- Repository/Client 分别隔离数据和下游。
- 能说明可测试、可复用、可维护的价值。

## 深度完善：面向 L6 的回答框架

围绕「如何避免所有业务逻辑堆在 Controller？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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
本题复习重点：如何避免所有业务逻辑堆在 Controller？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

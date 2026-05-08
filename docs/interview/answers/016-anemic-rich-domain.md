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

- 能定义贫血和充血。
- 能说出各自优缺点。
- 能说明核心交易适度充血。
- 能区分 Domain 和 Service 职责。
- 能避免“所有地方都 DDD”的过度设计。

## 深度完善：面向 L6 的回答框架

围绕「贫血模型和充血模型各有什么优缺点？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：贫血模型和充血模型各有什么优缺点？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

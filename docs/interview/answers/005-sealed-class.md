# 005 `sealed class` 适合建模哪些业务场景？

[返回按分类学习面试题](../README.md)

## 题目

`sealed class` 适合建模哪些业务场景？

## 先给面试官的短答案

`sealed class` 或 sealed interface 用来限制继承范围。它适合表达“类型有限且需要编译期约束”的业务模型，
例如支付结果、风控决策、订单操作结果、促销规则类型、领域事件类型。

它的价值不是语法新，而是让业务边界更明确。面试里可以这样说：

```text
如果某个业务概念只有有限几种合法类型，我会考虑 sealed interface。
例如风控决策只能是通过、拒绝、人工审核；支付渠道回调结果只能是成功、失败、处理中。
这样可以让编译器帮助限制非法扩展，并配合 switch 表达式做完整分支处理。
```

## 从零基础理解：为什么要限制继承？

普通接口可以被任何类实现：

```java
public interface RiskDecision {
}
```

任何人都可以写：

```java
public class UnknownDecision implements RiskDecision {
}
```

如果业务上只允许三种风控结果，这就太开放了。`sealed` 可以限制允许哪些类实现。

```java
public sealed interface RiskDecision
        permits RiskDecision.Pass, RiskDecision.Reject, RiskDecision.ManualReview {

    record Pass(String reason) implements RiskDecision {
    }

    record Reject(String reason) implements RiskDecision {
    }

    record ManualReview(String reason) implements RiskDecision {
    }
}
```

现在 `RiskDecision` 只有三种合法实现。

## 适合场景

### 风控决策

风控通常不是简单 true/false。常见结果：

- 通过。
- 拒绝。
- 人工审核。

```java
public sealed interface RiskDecision permits Pass, Reject, ManualReview {
}
```

这样订单服务可以明确处理每种结果。

### 支付结果

支付渠道返回结果可能是：

- 成功。
- 失败。
- 处理中。

如果只用字符串，容易出现拼写错误和遗漏处理。sealed 类型能让结果集合更稳定。

```java
public sealed interface PaymentChannelResult
        permits PaymentChannelResult.Success,
                PaymentChannelResult.Failure,
                PaymentChannelResult.Processing {

    record Success(String channelTradeNo) implements PaymentChannelResult {
    }

    record Failure(String reason) implements PaymentChannelResult {
    }

    record Processing(String message) implements PaymentChannelResult {
    }
}
```

### 运维操作结果

内部运维接口可能返回：

- 成功。
- 失败。
- 部分成功。

这类结果也适合 sealed 类型，因为调用方必须处理所有情况。

### 领域事件类型

如果某个模块内部只允许有限领域事件，可以使用 sealed interface。

```java
public sealed interface OrderDomainEvent permits OrderCreated, OrderPaid, OrderCancelled {
}
```

不过跨服务 MQ 事件还要考虑序列化、版本兼容和消费者语言，不要只从 Java 类型角度设计。

## 不适合场景

### 需要第三方自由扩展

如果你设计的是插件系统，允许第三方新增实现，sealed 反而会限制扩展。

例如支付渠道插件，如果业务允许外部团队不断新增渠道，普通接口可能更合适。

### 简单枚举就够了

如果状态没有额外数据，只是几个固定值，枚举更简单。

```java
public enum PaymentStatus {
    CREATED,
    SUCCEEDED,
    FAILED
}
```

如果每种类型有不同字段或行为，再考虑 sealed。

### 团队尚未熟悉新语法

架构设计要考虑团队维护能力。如果团队刚从 Java 8 升级，不要在所有地方大量使用 sealed。
可以先用于少数核心模型。

## 和 enum 怎么取舍？

用 enum：

- 类型只是固定名称。
- 每种类型没有不同字段结构。
- 主要用于状态持久化或简单分支。

用 sealed：

- 每种类型携带不同数据。
- 每种类型可能有不同方法。
- 希望编译器限制合法子类型。

例子：

```java
public enum RiskDecisionType {
    PASS,
    REJECT,
    MANUAL_REVIEW
}
```

如果只需要类型，用 enum 即可。

如果需要不同数据：

```java
record Reject(String reason, String ruleId) implements RiskDecision {
}

record ManualReview(String reason, String reviewerGroup) implements RiskDecision {
}
```

sealed 更合适。

## 在 eMall 项目中怎么讲？

eMall 中适合 sealed 的候选点：

- 风控决策结果。
- 支付渠道回调解析结果。
- 内部运维操作结果。
- 促销计算结果。
- 发布策略结果，例如继续放量、暂停、回滚。

比如发布系统：

```java
public sealed interface ReleaseDecision
        permits ReleaseDecision.Continue, ReleaseDecision.Pause, ReleaseDecision.Rollback {

    record Continue(String reason) implements ReleaseDecision {
    }

    record Pause(String reason) implements ReleaseDecision {
    }

    record Rollback(String reason) implements ReleaseDecision {
    }
}
```

这比返回字符串 `"continue"`、`"pause"`、`"rollback"` 更安全。

## 常见追问

### sealed class 是否会影响扩展性？

会。它的目的就是限制扩展。所以只适合业务类型集合本来就应该受控的地方。

专家回答：

```text
sealed 不是默认选择。开放扩展点用普通接口；受控业务集合用 sealed。
```

### sealed 能不能和 switch 配合？

可以，并且很适合。因为类型集合有限，`switch` 可以覆盖所有合法类型。

### 专家级完整回答

```text
sealed class 适合表达有限、受控的业务类型集合。比如风控决策只有通过、拒绝、人工审核；
支付渠道结果只有成功、失败、处理中。用 sealed interface 可以让编译器限制合法实现，
避免业务上不允许的类型被随意加入。

我会在核心领域模型中谨慎使用 sealed。简单固定状态用 enum 就够了；
如果每种类型携带不同数据或行为，并且扩展必须受控，就适合 sealed。
对于插件系统或第三方扩展点，我不会使用 sealed，因为它会限制扩展性。
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

- 知道 sealed 用来限制继承范围。
- 能说出适合有限业务类型集合。
- 能和 enum、普通接口做取舍。
- 能结合风控、支付、发布、运维等业务。
- 能指出不适合开放插件扩展点。

## 深度完善：面向 L6 的回答框架

围绕「`sealed class` 适合建模哪些业务场景？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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
本题复习重点：`sealed class` 适合建模哪些业务场景？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

# 037 如何设计稳定的公共库 API？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

如何设计稳定的公共库 API？

## 先给面试官的短答案

公共库 API 一旦被多个模块依赖，变更成本会很高。设计时要保持最小暴露、语义清晰、
输入输出稳定、错误模型明确、向后兼容，并配套文档和测试。

公共库不是为了“大家都能放代码”，而是提供稳定基础能力。

## 设计原则

### 暴露最小能力

只暴露调用方真正需要的接口，不暴露内部实现类。

### 参数对象优于长参数列表

不好：

```java
replay(String service, String status, int limit, String operator, String traceId);
```

更好：

```java
replayOutbox(ReplayOutboxCommand command);
```

### 返回明确结果

不要返回裸 `Map<String, Object>`。

```java
public record OperationResult(boolean success, int affected, String message) {
}
```

### 错误模型稳定

公共 API 应该明确抛什么异常或返回什么错误结果。

### 命名表达业务语义

`execute`、`handle`、`process` 太泛。
`publishOutboxEvents`、`authorizeInternalOperation` 更清楚。

## 公共库要避免什么？

- 放具体业务规则。
- 暴露可变内部对象。
- 让所有模块依赖所有东西。
- 频繁破坏性改接口。
- 使用不稳定枚举作为对外契约但不做兼容。
- 返回 Object 或 Map 逃避建模。

## 在 eMall 项目中怎么讲？

`common` 适合放：

- `ApiResponse`
- `ErrorCode`
- `BusinessException`
- Trace 工具。
- Outbox 基础设施接口。
- 审计记录模型。
- 加密接口。

不适合放：

- 订单支付规则。
- 库存扣减策略。
- 具体促销规则。
- 某个业务模块专属 SQL。

## 深度增强：工程化理解图

![Java 工程能力从语法到生产设计](../../assets/java-engineering-model.svg)

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

## 专家级完整回答

```text
稳定公共库 API 要最小暴露、语义清晰、输入输出强类型、错误模型稳定，并保证向后兼容。
公共库应该承载真正跨模块的基础能力，例如统一响应、错误码、审计、Outbox 基础设施和加密工具。
具体业务规则不应该进入 common，否则会让公共库变成强耦合中心。
```

## 回答评分点

高分答案应该覆盖：

- 最小暴露。
- 强类型输入输出。
- 稳定错误模型。
- 文档和测试。
- common 不应承载具体业务规则。

## 深度完善：面向 L6 的回答框架

围绕「如何设计稳定的公共库 API？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：如何设计稳定的公共库 API？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。


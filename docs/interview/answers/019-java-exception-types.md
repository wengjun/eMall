# 019 Java 异常分为哪些类型？

[返回按分类学习面试题](../README.md)

## 题目

Java 异常分为哪些类型？

## 先给面试官的短答案

Java 异常体系顶层是 `Throwable`，下面主要分为 `Error` 和 `Exception`。
`Error` 通常表示 JVM 或系统级严重问题，业务代码一般不应该捕获后继续运行。
`Exception` 又分 checked exception 和 unchecked exception。

在业务系统中，还会进一步区分参数异常、业务异常、下游异常和系统异常，并通过统一错误码返回。

## 基础结构

```text
Throwable
  Error
  Exception
    RuntimeException
```

### Error

常见：

- `OutOfMemoryError`
- `StackOverflowError`
- `NoClassDefFoundError`

这些通常表示严重问题。业务代码不应该简单 catch 住然后假装恢复。

### checked exception

编译器要求处理。

例如：

- `IOException`
- `SQLException`

方法签名中通常要 `throws`，调用方必须 catch 或继续抛。

### unchecked exception

继承 `RuntimeException`，编译器不强制处理。

例如：

- `NullPointerException`
- `IllegalArgumentException`
- `IllegalStateException`
- 自定义 `BusinessException`

## 业务系统中的异常分类

### 参数异常

用户输入不合法：

- 数量小于 1。
- requestId 为空。
- skuId 格式错误。

通常返回 `400 BAD_REQUEST`。

### 业务异常

请求格式正确，但业务规则不允许：

- 库存不足。
- 订单不存在。
- 已支付订单不能取消。
- 支付金额不一致。

通常返回明确业务错误码。

### 下游异常

调用其他服务失败：

- 库存服务超时。
- 支付渠道不可用。
- Redis 超时。
- Kafka 发送失败。

需要结合重试、熔断、降级、补偿。

### 系统异常

代码或基础设施异常：

- 数据库连接失败。
- 空指针。
- 序列化失败。
- 配置缺失。

需要记录日志和告警。

## 为什么要分层处理？

如果所有异常都返回：

```json
{
  "message": "system error"
}
```

前端、客服、监控、排障都无法判断发生了什么。

如果所有异常都报警，库存不足这种正常业务拒绝也会产生噪音。

正确做法：

- 业务异常有稳定错误码。
- 系统异常记录错误日志和 traceId。
- 下游异常进入熔断、降级、补偿。
- 前端只看到安全、可理解的信息。

## 在 eMall 项目中怎么讲？

eMall 中可以用 `BusinessException` 携带 `ErrorCode`：

```java
throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid");
```

统一异常处理把它转成 API 响应。

这样 Controller 不需要到处 try-catch，错误响应统一，监控也能按错误码聚合。

## 常见追问

### checked exception 和 unchecked exception 哪个更好？

没有绝对。底层 IO 库常用 checked exception 强制处理外部失败。
业务服务通常使用 unchecked business exception，让 ControllerAdvice 统一转换，并让事务默认回滚。

### 能不能 catch Throwable？

通常不要。catch `Throwable` 会捕获 `Error`，可能掩盖严重 JVM 问题。
框架最外层可以做兜底日志，但业务代码不应该随意 catch。

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

- Throwable、Error、Exception、RuntimeException 结构。
- checked 和 unchecked 区别。
- 能结合业务异常、系统异常、下游异常。
- 能说明统一错误码和异常处理。
- 能指出不要随意捕获 Error/Throwable。

## 深度完善：面向 L6 的回答框架

围绕「Java 异常分为哪些类型？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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
本题复习重点：Java 异常分为哪些类型？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

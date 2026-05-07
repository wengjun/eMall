# 023 如何设计统一错误码？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

如何设计统一错误码？

## 先给面试官的短答案

统一错误码要稳定、可分类、可监控、可被前端和客服理解。它不是简单给异常起名字，
而是 API 契约、排障入口和业务指标的一部分。

我会按错误类型分层，例如参数错误、认证失败、权限不足、资源不存在、状态冲突、
限流、下游不可用、系统错误。HTTP 状态码表达协议层结果，业务错误码表达业务原因。

## 为什么需要统一错误码？

如果每个接口随便返回字符串：

```text
order not found
Order missing
no such order
```

前端无法稳定处理，监控也无法聚合。

统一错误码可以用于：

- 前端展示。
- 客服定位。
- 日志检索。
- 指标聚合。
- 告警规则。
- API 契约。
- 国际化文案映射。

## 推荐分类

常见错误码：

```text
BAD_REQUEST
UNAUTHORIZED
FORBIDDEN
NOT_FOUND
CONFLICT
TOO_MANY_REQUESTS
DOWNSTREAM_UNAVAILABLE
SYSTEM_BUSY
INTERNAL_ERROR
```

业务更细时可以按领域扩展：

```text
INSUFFICIENT_STOCK
ORDER_STATUS_CONFLICT
PAYMENT_AMOUNT_MISMATCH
COUPON_NOT_AVAILABLE
```

关键是不要无限膨胀。错误码越多，治理成本越高。

## HTTP 状态码和业务错误码如何配合？

HTTP 状态码表达协议层语义：

- 400：请求参数问题。
- 401：未认证。
- 403：无权限。
- 404：资源不存在。
- 409：业务状态冲突。
- 429：限流。
- 503：下游不可用或系统繁忙。
- 500：未预期系统错误。

业务错误码表达具体原因：

```json
{
  "code": "PAYMENT_AMOUNT_MISMATCH",
  "message": "Payment amount mismatch"
}
```

## 错误响应结构

推荐：

```json
{
  "success": false,
  "code": "ORDER_STATUS_CONFLICT",
  "message": "Order cannot be paid from CANCELLED",
  "traceId": "..."
}
```

生产中 `message` 要避免泄露内部细节。更安全的方式是返回用户可理解信息，
详细原因写日志。

## 错误码设计原则

- 稳定，不随便改名。
- 可分类。
- 可监控。
- 文案和 code 分离。
- 不暴露敏感内部实现。
- 文档化。
- 有测试覆盖。
- 老客户端能处理未知错误码。

## 在 eMall 项目中怎么讲？

eMall 中统一 `ErrorCode` 可以支持：

- `BusinessException` 携带错误码。
- `CommonExceptionHandlerSupport` 统一映射 HTTP 状态。
- 指标按错误码统计。
- 前端按错误码展示文案。
- 运维通过 traceId 和错误码排查。

例如：

```java
throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid");
```

## 常见追问

### 错误码越细越好吗？

不是。太粗无法定位，太细难治理。高频、需要前端特殊处理、需要监控聚合的错误才值得独立错误码。

### message 能不能给用户看？

业务错误可以给用户友好文案。系统错误不要暴露内部细节。

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
统一错误码是 API 契约和可观测性的一部分。我会让 HTTP 状态码表达协议层结果，
业务错误码表达具体业务原因，例如库存不足、订单状态冲突、支付金额不一致。

错误码要稳定、可分类、可监控，不能随意改名。响应中返回 code、message 和 traceId，
完整异常堆栈留在服务端日志。前端和多语言文案基于 code 映射，监控也按 code 聚合错误趋势。
```

## 回答评分点

高分答案应该覆盖：

- 错误码是契约和观测维度。
- 能区分 HTTP 状态码和业务错误码。
- 能设计响应结构。
- 能说明稳定性、分类、监控、国际化。
- 能说明错误码不能无限膨胀。

## 深度完善：面向 L6 的回答框架

围绕「如何设计统一错误码？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：如何设计统一错误码？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

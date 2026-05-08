# 021 checked exception 和 unchecked exception 如何取舍？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

checked exception 和 unchecked exception 如何取舍？

## 先给面试官的短答案

checked exception 会强制调用方在编译期处理，适合底层 API 明确要求调用方处理的外部资源失败，
例如文件、网络、IO。unchecked exception 不强制在签名中声明，更适合业务系统中的业务异常、
参数错误、状态冲突和不可恢复系统错误。

在 Spring 后端服务里，我通常用 unchecked 的 `BusinessException` 表达业务异常，
由统一异常处理转换成错误码和 HTTP 响应。这样 Service 方法签名保持业务语义，
事务默认也会对 unchecked exception 回滚。

## 从零基础理解

checked exception：

```java
public String readFile(String path) throws IOException {
    return Files.readString(Path.of(path));
}
```

调用方必须处理：

```java
try {
    readFile(path);
} catch (IOException ex) {
    // Handle error.
}
```

unchecked exception：

```java
throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid");
```

方法签名不强制写 `throws`，由上层统一处理。

## checked exception 的优点和缺点

优点：

- 编译器强制调用方意识到失败。
- 适合外部资源失败是 API 契约一部分的场景。
- 对底层库调用者更明确。

缺点：

- 多层业务代码中容易机械传递 `throws`。
- 方法签名被技术异常污染。
- Lambda 和 Stream 中处理较麻烦。
- 调用方经常只是包装再抛，实际价值有限。

## unchecked exception 的优点和缺点

优点：

- 业务方法签名更清晰。
- 可以由 `@ControllerAdvice` 统一转换响应。
- Spring 事务默认遇到 unchecked exception 回滚。
- 更适合领域规则冲突。

缺点：

- 编译器不强制处理。
- 如果没有统一异常治理，容易到处抛、到处 catch。
- 需要依赖测试和代码评审保证关键分支被处理。

## 在业务系统中怎么取舍？

### 底层基础设施可以保留 checked

例如文件导入、IO 读取、第三方 SDK 底层 API。

### 业务层转成明确业务异常

不要把 `SQLException`、`IOException` 一路抛到 Controller。

可以在基础设施层转换：

```java
try {
    jdbcTemplate.update(sql, args);
} catch (DataAccessException ex) {
    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "database operation failed", ex);
}
```

真实项目中系统异常可能用单独的 `SystemException` 或直接让框架处理，
关键是不要把底层异常原样暴露给用户。

## 和事务的关系

Spring 默认只对 unchecked exception 和 `Error` 回滚。
如果 checked exception 也要回滚，需要配置：

```java
@Transactional(rollbackFor = Exception.class)
```

所以在业务服务中使用 unchecked `BusinessException` 更常见。

## 在 eMall 项目中怎么讲？

例如订单状态不允许支付：

```java
throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid from " + order.status());
```

这是业务异常，用 unchecked 更合适。

库存服务超时、支付渠道失败，可以转换成下游不可用错误码，再由补偿、熔断、告警处理。

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

## 专家级完整回答

```text
checked exception 适合底层 API 把外部资源失败作为契约强制调用方处理，
但在业务服务层，如果大量 checked exception 穿透方法签名，会污染业务语义。

在 Spring 分布式服务中，我更倾向用 unchecked BusinessException 表达库存不足、
订单状态冲突、支付金额不一致这类业务错误，并通过 ControllerAdvice 统一转换响应。
系统异常和下游异常则记录日志、指标和 traceId，必要时触发熔断、补偿或告警。
如果 checked exception 需要触发事务回滚，要显式配置 rollbackFor。
```

## 回答评分点

高分答案应该覆盖：

- 能解释 checked 和 unchecked 的编译期差异。
- 能说明底层资源失败和业务异常的不同。
- 能联系 Spring 事务默认回滚规则。
- 能说明统一异常处理的重要性。
- 能避免把底层异常直接暴露到 API。

## 深度完善：面向 L6 的回答框架

围绕「checked exception 和 unchecked exception 如何取舍？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：checked exception 和 unchecked exception 如何取舍？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

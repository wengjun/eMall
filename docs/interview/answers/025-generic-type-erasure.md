# 025 泛型擦除是什么？

[返回按分类学习面试题](../README.md)

## 题目

泛型擦除是什么？

## 先给面试官的短答案

Java 泛型主要在编译期提供类型检查。编译后，大部分泛型类型信息会被擦除，
运行时通常只知道原始类型，不知道 `List<String>` 还是 `List<Long>`。

这就是泛型擦除。它影响反射、序列化、泛型数组、运行时类型判断和通用框架设计。

## 从零基础理解

写代码时：

```java
List<String> names = new ArrayList<>();
List<Long> ids = new ArrayList<>();
```

编译器会检查：

```java
names.add("alice"); // ok
names.add(1L);      // compile error
```

但运行时，这两个对象主要都是 `ArrayList`。

你不能这样判断：

```java
if (names instanceof List<String>) {
}
```

因为运行时没有完整的 `List<String>` 信息。

## 为什么 Java 要擦除？

主要是历史兼容。Java 早期没有泛型，后来为了兼容老代码和 JVM 字节码模型，引入了类型擦除方案。

好处：

- 兼容旧版本集合代码。
- 不需要为每种泛型生成一份新类。

代价：

- 运行时泛型信息不完整。
- 某些代码写起来不直观。

## 泛型擦除带来的限制

### 不能直接 new T

```java
public class Factory<T> {
    public T create() {
        return new T(); // compile error
    }
}
```

因为运行时不知道 T 是什么。

通常要传入：

```java
private final Class<T> type;
```

### 不能创建泛型数组

```java
T[] values = new T[10]; // compile error
```

### 不能判断参数化类型

```java
if (value instanceof List<String>) {
}
```

不允许。

### JSON 反序列化需要额外类型信息

如果要反序列化：

```java
ApiResponse<List<OrderResponse>>
```

运行时需要 `TypeReference` 这类方式保留泛型信息。

## 在后端工程中的影响

### 通用 API 响应

```java
public record ApiResponse<T>(boolean success, T data, String code, String message) {
}
```

序列化时通常没问题，但反序列化 `ApiResponse<List<OrderResponse>>` 时要传完整类型。

### 通用 Repository 或 Client

如果写通用 HTTP 客户端：

```java
public <T> T get(String url, Class<T> type) {
}
```

简单类型可以用 `Class<T>`，复杂泛型要用 `ParameterizedTypeReference<T>`。

### 事件反序列化

MQ 消费时，如果 payload 是泛型对象，必须明确事件类型和 schema。

## 常见追问

### 泛型擦除后为什么还能知道方法返回 List<String>？

class 文件里可能保留 Signature 元数据，反射有时能读到声明信息。
但对象实例本身通常不知道自己是 `List<String>`。

### 泛型是不是没有用？

不是。泛型在编译期非常有用，可以提前发现类型错误，减少强制类型转换。

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
Java 泛型主要是编译期类型安全机制，编译后大部分泛型类型会被擦除。
所以运行时通常只能看到原始类型，例如 ArrayList，而不知道它是 List<String> 还是 List<Long>。

这会影响 new T、泛型数组、instanceof 参数化类型和 JSON 反序列化。
在后端框架或通用 HTTP 客户端中，如果要处理 ApiResponse<List<OrderDto>>，
需要 TypeReference 或 ParameterizedTypeReference 保留完整类型信息。
```

## 回答评分点

高分答案应该覆盖：

- 泛型主要提供编译期类型安全。
- 运行时大部分泛型信息被擦除。
- 能举 `List<String>` 和 `List<Long>` 例子。
- 能说明 new T、泛型数组、instanceof 限制。
- 能联系 JSON 反序列化和通用客户端。

## 深度完善：面向 L6 的回答框架

围绕「泛型擦除是什么？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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
本题复习重点：泛型擦除是什么？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

# 006 `Optional` 应该用在返回值、参数还是字段上？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

`Optional` 应该用在返回值、参数还是字段上？

## 先给面试官的短答案

`Optional` 最适合用作方法返回值，用来明确表达“结果可能不存在”。
不推荐用作字段，也不推荐用作方法参数。

例如 Repository 查询单条记录：

```java
Optional<Order> findById(long orderId);
```

这能强迫调用方处理不存在的情况。

但下面这种不推荐：

```java
public void update(Optional<String> nickname) {
}

public class User {
    private Optional<String> mobile;
}
```

## 从零基础理解：`Optional` 解决什么问题？

Java 中最常见的问题之一是空指针：

```java
Order order = orderRepository.findById(orderId);
System.out.println(order.status()); // order 如果是 null，会抛 NullPointerException
```

`Optional` 的目的是让“可能为空”变得显式。

```java
Optional<Order> order = orderRepository.findById(orderId);
```

调用方一看就知道：这个订单可能不存在。

## 推荐：作为返回值

Repository 层非常适合：

```java
Optional<UserAccount> findByMobile(String mobile);
Optional<Order> findByRequestId(String requestId);
Optional<PaymentOrder> findByChannelTradeNo(String channelTradeNo);
```

调用方可以决定怎么处理：

```java
Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "order not found"));
```

这比返回 null 更清楚，也更不容易漏判断。

## 不推荐：作为参数

不好的写法：

```java
public void updateNickname(long userId, Optional<String> nickname) {
}
```

问题：

- 调用方仍然可以传 `null`。
- API 变得啰嗦。
- 不如重载方法或使用明确命令对象。

更好的写法：

```java
public record UpdateUserCommand(long userId, String nickname) {
}
```

如果字段可选，可以在命令对象中用 null 表达“未提供”，或者使用更明确的 patch 结构。
关键是团队要统一约定。

## 不推荐：作为字段

不好的写法：

```java
public class UserAccount {
    private Optional<String> mobile;
}
```

问题：

- ORM 映射不自然。
- JSON 序列化可能变复杂。
- 字段本身仍然可能是 null。
- 对象模型变得啰嗦。

字段可以直接使用真实类型：

```java
private String mobile;
```

然后在方法返回时用 `Optional` 表达不存在：

```java
public Optional<String> mobile() {
    return Optional.ofNullable(mobile);
}
```

## 业务系统中的使用边界

### Repository 返回 Optional

适合，因为查不到是正常情况。

```java
Optional<Order> findByRequestId(String requestId);
```

下单幂等逻辑可以这样写：

```java
return orderRepository.findByRequestId(requestId)
        .orElseGet(() -> createOnce(requestId, userId, skuId, quantity));
```

### Service 是否返回 Optional？

要看语义。

如果业务语义允许不存在，可以返回 `Optional`：

```java
Optional<Coupon> findAvailableCoupon(long userId);
```

如果业务要求必须存在，Service 可以直接抛业务异常：

```java
public Order get(long orderId) {
    return orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "order not found"));
}
```

### Controller 不建议直接暴露 Optional

HTTP API 应该返回明确响应。不要让 JSON 里出现奇怪的 Optional 结构。

## 常见坑

### 滥用 `get()`

```java
Order order = optionalOrder.get();
```

如果 Optional 为空，会抛 `NoSuchElementException`。这和空指针差不多，只是换了异常。

更好：

```java
Order order = optionalOrder.orElseThrow(...);
```

### 用 Optional 包装集合

不推荐：

```java
Optional<List<Order>> findOrders(long userId);
```

空集合就能表达没有订单：

```java
List<Order> findOrders(long userId);
```

### 在性能热点中过度创建

`Optional` 是对象。大多数业务场景不用担心，但极端性能热点里不要为了形式滥用。

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
Optional 最适合作为返回值，用来表达查询结果可能不存在。
例如 Repository 的 findById、findByRequestId、findByChannelTradeNo 返回 Optional，
调用方必须显式处理不存在分支。

我不推荐把 Optional 用作参数或实体字段。参数使用 Optional 会让 API 啰嗦，
而且调用方仍然能传 null；字段使用 Optional 会影响 ORM、序列化和对象模型清晰度。

在 Service 层，我会根据业务语义选择：如果不存在是正常业务分支，可以返回 Optional；
如果不存在就是业务错误，则转成明确的 BusinessException 和错误码。
```

## 回答评分点

高分答案应该覆盖：

- 推荐作为返回值。
- 不推荐作为参数和字段。
- 能解释 Repository 和 Service 层差异。
- 能指出不要滥用 `get()`。
- 能指出集合返回空集合即可。
- 能结合幂等查询和业务异常。

## 深度完善：面向 L6 的回答框架

围绕「`Optional` 应该用在返回值、参数还是字段上？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：`Optional` 应该用在返回值、参数还是字段上？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

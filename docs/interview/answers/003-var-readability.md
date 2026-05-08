# 003 `var` 会不会影响可读性，团队中如何约束？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

`var` 会不会影响可读性，团队中如何约束？

## 先给面试官的短答案

`var` 本身不会让 Java 变成动态语言，它只是局部变量类型推断，变量类型仍然在编译期确定。
它是否影响可读性，取决于右侧表达式和变量名是否足够清楚。

我会这样约束：

- 只在局部变量中使用 `var`。
- 右侧构造器、静态工厂或方法名必须能清楚表达类型。
- 公共 API 的参数、返回值、字段不用 `var`，也不能用。
- 复杂业务代码里，优先让变量名表达业务含义，而不是追求少写类型。
- Code Review 中发现 `var data = call()` 这类语义不清的写法要改回显式类型。

面试回答可以这样说：

```text
我不把 var 当成炫技语法，而是当成降低局部噪音的工具。
如果右侧表达式已经清楚说明类型，var 能提升阅读体验；
如果类型是理解业务的关键信息，就应该显式写出来。
团队里需要通过代码规范和 Code Review 限制它的使用边界。
```

## 从零基础理解：`var` 是什么？

传统 Java 写局部变量需要显式声明类型：

```java
Order order = orderService.get(orderId);
List<OutboxEvent> events = outboxRepository.findReadyToPublish(100);
```

使用 `var` 后可以写成：

```java
var order = orderService.get(orderId);
var events = outboxRepository.findReadyToPublish(100);
```

编译器会根据右侧表达式推断类型。这里 `order` 仍然是 `Order`，`events` 仍然是
`List<OutboxEvent>`。运行时没有“动态类型”。

这点很重要。很多刚接触 Java 的工程师会误以为 `var` 像 JavaScript 的 `var`。
实际上 Java 的 `var` 是静态类型推断，不是弱类型。

## 什么时候 `var` 能提升可读性？

### 右侧类型已经很明显

```java
var request = new CreateOrderRequest("req-1", 10001L, 20001L, 1);
var amount = new BigDecimal("99.00");
```

这里右侧已经明确告诉读者类型，重复写一遍类型价值不大。

### 泛型类型很长

```java
Map<Long, List<OrderLine>> linesBySku = orderLines.stream()
        .collect(Collectors.groupingBy(OrderLine::skuId));
```

可以写成：

```java
var linesBySku = orderLines.stream()
        .collect(Collectors.groupingBy(OrderLine::skuId));
```

如果变量名清楚，`var` 能减少视觉噪音。

### 链式调用结果由方法名表达业务语义

```java
var pendingEvents = outboxRepository.findReadyToPublish(100);
var paidOrder = order.markPaid();
```

`pendingEvents`、`paidOrder` 本身就表达了业务含义。

## 什么时候 `var` 会伤害可读性？

### 方法名和变量名都含糊

```java
var result = client.call(input);
var data = mapper.map(value);
```

读者不知道 `result` 是 HTTP 响应、业务结果、异常包装还是 DTO。
这种代码在大型项目中会增加跳转成本。

更好的写法：

```java
PaymentCallbackResult callbackResult = paymentClient.verifyCallback(request);
```

### 类型本身是业务信息

```java
var id = idGenerator.nextId();
```

这里 `id` 是 `long`、`String` 还是自定义 `OrderId`？如果类型影响数据库、JSON 或日志格式，
显式写出来更好。

```java
long orderId = idGenerator.nextId();
```

### 多态返回值需要强调接口或实现

```java
var repository = createRepository();
```

如果 `repository` 是 `OrderRepository` 接口还是 `JdbcOrderRepository` 实现会影响后续理解，
最好显式写清。

## 团队规范应该怎么定？

推荐规范：

```text
1. 只允许局部变量使用 var。
2. 右侧是 new Xxx(...) 时可以使用。
3. 右侧方法名能表达明确业务类型时可以使用。
4. 变量名必须表达业务含义，禁止 var data、var result、var obj 这类模糊命名。
5. 涉及金额、ID、时间、状态、泛型边界时，如果类型影响理解，优先显式类型。
6. Lambda 参数、字段、方法签名不能用 var 逃避设计。
```

Code Review 可以问两个问题：

- 不跳转代码，能否知道变量是什么？
- 显式类型是否比 `var` 更能帮助理解业务？

如果答案是否定的，就不要用 `var`。

## 在 eMall 项目中怎么用？

适合：

```java
var order = orderService.get(orderId);
var reservation = inventoryClient.reserve(request);
var event = OutboxEvent.create(eventId, aggregateType, aggregateId, eventType, payload);
```

不适合：

```java
var result = downstream.call(request);
var value = repository.find(id);
```

电商核心链路里，金额、状态、ID、时间是重要业务信息。比如：

```java
BigDecimal payableAmount = pricingResult.payableAmount();
Instant paidAt = Instant.now();
OrderStatus status = order.status();
```

这些地方显式类型能帮助读者快速理解业务语义。

## 常见追问

### `var` 会不会影响性能？

不会。`var` 是编译期语法，编译后变量仍然是明确类型，不会引入运行时性能损耗。

### `var` 会不会影响重构？

有时会帮助，有时会伤害。方法返回类型变了，`var` 可能让局部代码继续编译，
但业务语义可能已经变了。因此公共 API 变更仍然要靠测试和 Code Review。

### 专家级完整回答

```text
var 是局部变量类型推断，不是动态类型。它的价值是减少局部变量声明中的重复噪音，
但前提是右侧表达式和变量名已经足够清楚。

在团队实践中，我会限制 var 只用于局部变量，并要求变量名表达业务含义。
对于金额、ID、时间、状态、接口类型这类类型本身有业务意义的地方，我倾向于显式声明。
在电商系统里，可读性比少写几个字符更重要，因为核心交易代码需要长期维护和审计。
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

- 知道 `var` 是编译期局部变量类型推断。
- 能说明它不是 JavaScript 的动态类型。
- 能讲清楚适合和不适合场景。
- 能提出团队规范和 Code Review 标准。
- 能关联金额、ID、状态这类业务类型的重要性。

## 深度完善：面向 L6 的回答框架

围绕「`var` 会不会影响可读性，团队中如何约束？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：`var` 会不会影响可读性，团队中如何约束？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

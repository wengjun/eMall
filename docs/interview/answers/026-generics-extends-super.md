# 026 泛型通配符 `extends` 和 `super` 怎么理解？

[返回按分类学习面试题](../README.md)

## 题目

泛型通配符 `extends` 和 `super` 怎么理解？

## 先给面试官的短答案

可以用 PECS 原则理解：Producer Extends, Consumer Super。
如果一个泛型结构主要生产数据给你读取，用 `? extends T`；
如果它主要消费你写入的数据，用 `? super T`。

简单说：

- `? extends Order`：可以安全读出 Order，但不适合写入具体对象。
- `? super PaidOrder`：可以安全写入 PaidOrder，但读出来只能当 Object 或上界处理。

## 从零基础理解

假设：

```java
class Order {
}

class PaidOrder extends Order {
}
```

### `extends` 适合读取

```java
void printOrders(List<? extends Order> orders) {
    for (Order order : orders) {
        System.out.println(order);
    }
}
```

调用方可以传：

```java
List<Order>
List<PaidOrder>
```

因为不管里面具体是哪种 Order 子类，读出来都至少是 Order。

但你不能安全 add 一个普通 `Order`：

```java
orders.add(new Order()); // compile error
```

因为实际传入的可能是 `List<PaidOrder>`。

### `super` 适合写入

```java
void addPaidOrder(List<? super PaidOrder> orders, PaidOrder paidOrder) {
    orders.add(paidOrder);
}
```

调用方可以传：

```java
List<PaidOrder>
List<Order>
List<Object>
```

因为这些集合都能接收一个 PaidOrder。

但读出来时类型只能安全看作 Object：

```java
Object value = orders.get(0);
```

## PECS 原则

```text
Producer Extends
Consumer Super
```

意思是：

- 你从它里面读数据，它是生产者，用 extends。
- 你往它里面写数据，它是消费者，用 super。

## 后端工程中的例子

### 批量处理订单只读

```java
void exportOrders(List<? extends Order> orders) {
    for (Order order : orders) {
        // Read order data.
    }
}
```

### 收集处理结果

```java
void collectPaidOrders(List<? super PaidOrder> target, PaidOrder paidOrder) {
    target.add(paidOrder);
}
```

## 不要过度使用复杂泛型

业务代码可读性很重要。如果泛型写得太复杂，新人很难理解。

公共库和框架层可以适当使用通配符提升扩展性；普通业务代码优先简单清晰。

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
extends 和 super 可以用 PECS 理解。一个集合如果主要被我读取，它生产 T，
就用 ? extends T；如果主要被我写入，它消费 T，就用 ? super T。

? extends Order 能让我安全读出 Order，但不能安全写入具体 Order；
? super PaidOrder 能让我安全写入 PaidOrder，但读出来只能当 Object 处理。
在公共库 API 中合理使用通配符能提升扩展性，但业务代码要避免过度复杂。
```

## 回答评分点

高分答案应该覆盖：

- 能说出 PECS。
- 能解释 extends 适合读。
- 能解释 super 适合写。
- 能举父类子类集合例子。
- 能强调业务代码可读性。

## 深度完善：面向 L6 的回答框架

围绕「泛型通配符 `extends` 和 `super` 怎么理解？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：泛型通配符 `extends` 和 `super` 怎么理解？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

# 008 `BigDecimal` 的 `equals` 和 `compareTo` 有什么区别？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

`BigDecimal` 的 `equals` 和 `compareTo` 有什么区别？

## 先给面试官的短答案

`BigDecimal.equals` 会同时比较数值和 scale；`compareTo` 只比较数值大小。
金额业务判断通常应该用 `compareTo`，否则 `1.0` 和 `1.00` 会被 `equals` 判断为不相等。

示例：

```java
BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");

System.out.println(a.equals(b));    // false
System.out.println(a.compareTo(b)); // 0
```

## 从零基础理解：scale 是什么？

`BigDecimal` 不只是保存一个数字，还保存小数位数。

```java
new BigDecimal("1.0")
```

数值是 1，小数位是 1。

```java
new BigDecimal("1.00")
```

数值也是 1，但小数位是 2。

所以 `equals` 认为它们不是完全相同对象；`compareTo` 认为它们数值相等。

## 金额比较为什么用 `compareTo`？

支付渠道可能返回 `100.0`，订单库保存 `100.00`。
业务上它们金额相同。如果用 `equals`，可能误判金额不一致。

正确写法：

```java
if (paidAmount.compareTo(orderAmount) != 0) {
    throw new BusinessException(ErrorCode.CONFLICT, "paid amount mismatch");
}
```

判断大于、小于也用 `compareTo`：

```java
if (payableAmount.compareTo(BigDecimal.ZERO) <= 0) {
    throw new BusinessException(ErrorCode.CONFLICT, "payable amount must be positive");
}
```

## `equals` 什么时候有用？

如果你确实需要判断数值和 scale 都一致，可以使用 `equals`。

例如某些格式化、序列化、测试场景中，`1.0` 和 `1.00` 代表不同输入格式。
但金额业务里通常不应该这样判断。

## `HashMap` 和 `HashSet` 中的坑

因为 `equals` 不同，hash 也可能不同。

```java
Set<BigDecimal> set = new HashSet<>();
set.add(new BigDecimal("1.0"));
set.add(new BigDecimal("1.00"));

System.out.println(set.size()); // 2
```

如果你希望按数值去重，需要先规范化：

```java
BigDecimal normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
```

或者使用业务封装对象统一规则。

## 金额入库前是否要统一 scale？

建议统一。比如人民币金额统一两位小数：

```java
BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
```

但不同币种的小数位不同，生产系统要结合币种元数据：

- CNY：2 位。
- JPY：0 位。
- KWD：3 位。

不要在多币种系统里硬编码所有金额都是 2 位。

## 在 eMall 项目中怎么讲？

支付回调金额校验应该使用：

```java
payment.amount().compareTo(paidAmount) != 0
```

而不是：

```java
!payment.amount().equals(paidAmount)
```

因为支付渠道返回的 scale 可能和本地保存不同。业务只关心数值是否一致。

## 常见追问

### `stripTrailingZeros` 能解决吗？

可以规范化部分场景：

```java
new BigDecimal("1.00").stripTrailingZeros()
```

但它可能产生科学计数形式，也不代表业务 scale 规则。金额系统更推荐统一 scale 和币种规则。

### `BigDecimal.ZERO.equals(new BigDecimal("0.00"))` 是什么？

结果是 false，因为 scale 不同。业务判断是否为 0 应该：

```java
amount.compareTo(BigDecimal.ZERO) == 0
```

### 专家级完整回答

```text
BigDecimal.equals 比较数值和 scale，compareTo 只比较数值大小。
金额业务通常使用 compareTo，因为 100.0 和 100.00 在业务上是同一金额。

在支付回调、退款、对账中，如果用 equals，可能因为渠道返回的小数位和本地保存不同而误判。
我会在金额入库和跨服务传输时统一 scale、rounding mode 和币种规则，
比较时用 compareTo，只有在确实需要比较格式时才使用 equals。
```

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

## 回答评分点

高分答案应该覆盖：

- 能解释 scale。
- 能举出 `1.0` 和 `1.00` 的例子。
- 能说明金额比较用 `compareTo`。
- 能指出 HashSet/HashMap 的坑。
- 能讲统一 scale、rounding mode 和币种。

## 深度完善：面向 L6 的回答框架

围绕「`BigDecimal` 的 `equals` 和 `compareTo` 有什么区别？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：`BigDecimal` 的 `equals` 和 `compareTo` 有什么区别？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。


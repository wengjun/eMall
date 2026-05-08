# 007 为什么金额不能用 `double`？

[返回按分类学习面试题](../README.md)

## 题目

为什么金额不能用 `double`？

## 先给面试官的短答案

金额不能用 `double`，因为 `double` 是二进制浮点数，很多十进制小数无法精确表示。
电商系统中的价格、优惠、支付、退款、结算、对账都要求精确、可重复、可审计。

Java 中金额通常用 `BigDecimal`，数据库中用 `DECIMAL`，跨服务传输可以用字符串或最小货币单位整数。

面试回答：

```text
金额链路不能接受浮点误差。一分钱误差在下单、支付、退款、结算和对账中都会变成生产问题。
所以我会用 BigDecimal 或以分为单位的 long，并统一 scale、rounding mode 和币种。
```

## 从零基础理解：`double` 为什么不精确？

计算机底层用二进制。十进制里的 `0.1`，用二进制无法精确表示。

示例：

```java
System.out.println(0.1 + 0.2);
```

输出可能是：

```text
0.30000000000000004
```

这对普通科学计算可能可以接受，但对金额不行。

如果用户应付 0.30 元，系统算出 0.30000000000000004，后续比较、入库、对账都可能出问题。

## 电商系统里金额有哪些？

不仅商品价格是金额。电商系统里金额非常多：

- 商品单价。
- 商品总价。
- 优惠金额。
- 运费。
- 税费。
- 应付金额。
- 实付金额。
- 退款金额。
- 商家结算金额。
- 平台佣金。
- 渠道手续费。
- 对账差异金额。

这些金额会跨订单、支付、财务、结算、对账多个系统流转。任何一个环节误差都可能导致用户投诉或财务事故。

## 正确做法一：使用 `BigDecimal`

推荐：

```java
BigDecimal unitPrice = new BigDecimal("99.00");
BigDecimal quantity = new BigDecimal("2");
BigDecimal subtotal = unitPrice.multiply(quantity);
```

不要这样写：

```java
BigDecimal wrong = new BigDecimal(0.1);
```

因为 `0.1` 先被表示成不精确的 double，再传给 BigDecimal。

更安全：

```java
BigDecimal right = new BigDecimal("0.10");
BigDecimal alsoRight = BigDecimal.valueOf(0.1);
```

## 正确做法二：使用最小货币单位整数

很多支付系统会用“分”为单位：

```java
long amountInCents = 9900L; // 99.00 元
```

优点：

- 比较和加减简单。
- 没有小数精度问题。
- 跨语言传输稳定。

缺点：

- 多币种小数位不同，需要货币元数据。
- 折扣、税费、汇率计算仍然需要明确舍入规则。

## 数据库应该怎么存？

MySQL 中推荐：

```sql
payable_amount decimal(19, 2) not null
```

不要用：

```sql
payable_amount double
```

如果使用最小单位整数：

```sql
payable_amount_cents bigint not null
currency varchar(3) not null
```

无论哪种方式，都要明确币种。

## 舍入规则很重要

金额计算不能只说用 `BigDecimal`，还要统一舍入规则。

例如：

```java
BigDecimal avg = total.divide(count, 2, RoundingMode.HALF_UP);
```

如果不指定舍入模式，某些除法会抛异常：

```java
new BigDecimal("10").divide(new BigDecimal("3"));
```

因为 10 / 3 是无限小数。

生产系统要统一：

- scale。
- rounding mode。
- 计算顺序。
- 优惠分摊规则。
- 退款分摊规则。

## 在 eMall 项目中怎么讲？

eMall 的订单服务保存：

- 单价快照。
- 商品小计。
- 优惠金额。
- 应付金额。
- 币种。
- 价格版本。

支付服务校验回调金额：

```java
if (payment.amount().compareTo(paidAmount) != 0) {
    throw new BusinessException(ErrorCode.CONFLICT, "paid amount mismatch");
}
```

这体现了两个重点：

- 金额用 `BigDecimal`。
- 金额比较用 `compareTo`，不是 `equals`。

## 常见追问

### 金额一定用 BigDecimal 吗？

不一定。也可以用最小货币单位整数。关键是不能用浮点数，并且要统一币种和舍入规则。

### BigDecimal 是否有性能问题？

它比 `double` 慢，但金额计算通常不是电商系统最大瓶颈。正确性优先。
如果是超高频金融计算，可以进一步优化模型，但仍不能牺牲精度。

### 专家级完整回答

```text
金额不能用 double，因为 double 是二进制浮点数，无法精确表示很多十进制小数。
电商金额涉及下单、优惠、支付、退款、结算和对账，必须精确、可重复、可审计。

我通常会选择 BigDecimal 或最小货币单位 long。数据库用 DECIMAL 或 bigint cents，
跨服务传输用字符串或整数。同时必须统一 scale、rounding mode、币种和分摊规则。
只说用 BigDecimal 还不够，金额比较、除法、优惠分摊和对账规则都要统一。
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

- 能解释二进制浮点误差。
- 能说明金额链路的生产风险。
- 能说出 BigDecimal 和最小单位整数两种方案。
- 能提醒 BigDecimal 构造方式。
- 能强调舍入规则、币种和对账。

## 深度完善：面向 L6 的回答框架

围绕「为什么金额不能用 `double`？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：为什么金额不能用 `double`？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

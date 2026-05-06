# 007 为什么金额不能用 `double`？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

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

- 能解释二进制浮点误差。
- 能说明金额链路的生产风险。
- 能说出 BigDecimal 和最小单位整数两种方案。
- 能提醒 BigDecimal 构造方式。
- 能强调舍入规则、币种和对账。

## 二次深度补强

题目：为什么金额不能用 `double`？

二次补强标记：已完成

### 面试官真正想确认的能力

语言特性不能孤立背诵，要落到业务建模、兼容性、可测试性和团队规范。
围绕这道题，要进一步把概念、项目实现、线上风险和验证闭环连起来。

### 深度和广度补充

- 先解释语法或 API 的基本语义，再说明它解决了什么工程问题。
- 把例子落到订单、金额、时间、状态、DTO、异常或领域模型。
- 主动说明误用风险，例如可读性下降、精度错误、兼容性破坏。
- 最后给出团队规范、代码评审和自动化测试的落地方式。

### 图片讲解

![二次补强图解](../../assets/java-engineering-model.svg)

- 图中从语法层、模型层、协作层到生产层逐层展开。
- 回答时要把本题放进这条链路，而不是只停在语法定义。
- 能说明边界和治理，才像生产项目负责人，而不是只会写功能代码。

### Java17 工程建模示例

```java
import java.util.Objects;

public record EngineeringRule(String name, String decision, boolean backwardCompatible) {

    public EngineeringRule {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(decision, "decision");
        if (name.isBlank() || decision.isBlank()) {
            throw new IllegalArgumentException("Rule metadata must be explicit.");
        }
    }
}

final class EngineeringRuleReviewer {

    boolean canRelease(EngineeringRule rule) {
        return rule.backwardCompatible() && !rule.decision().contains("global mutable state");
    }
}
```

### 高分表达要点

- 不要只回答定义，要说明为什么这样设计、在什么条件下失效、如何监控和回滚。
- 把答案和当前电商项目联系起来，例如订单、库存、支付、履约、搜索、风控或发布链路。
- 主动给出边界条件和反例，能让面试官看到你具备生产系统判断力。

## 逐题专项补强

逐题专项补强标记：已完成

### 本题专项切入

- 本题要围绕「为什么金额不能用 `double`？」展开，不要只复述分类模板。
- 先解释 Java 语义，再说明它怎样降低电商项目中的误用成本。
- 重点补充边界条件、反例、代码规范和自动化测试。

### 专项图解说明

![逐题专项图解](../../assets/java-engineering-model.svg)

- 这张图用于把「为什么金额不能用 `double`？」放回生产链路中理解，重点看入口、状态、数据和恢复闭环。
- 面试时可以先按图说明主路径，再补失败路径、监控指标和回滚手段。

### 贴合本题的实现示例

```java
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) {

    public Money {
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    Money add(Money other) {
        return new Money(amount.add(other.amount()));
    }
}
```

### 进一步追问时的回答边界

- 如果面试官继续追问，要主动说明这个实现是核心模型，不等于完整生产组件。
- 生产级落地还需要接入鉴权、幂等、限流、熔断、监控、告警、灰度和数据修复。
- 回答时把复杂度、失败场景、验证方式和 eMall 项目中的落地位置一起说清楚。

## 面试实战补强

面试实战补强标记：已完成

### 面试追问路线

- 为什么这个语言特性在生产代码里能减少错误，而不是只减少代码行数？
- 如果团队成员滥用这个特性，你会用什么规范、评审和测试约束？
- 这个特性在订单、金额、时间、状态建模里有哪些反例？

### eMall 项目落点

- 可以落到模块：common、order、inventory、payment。
- 回答「为什么金额不能用 `double`？」时，要从这些模块里选一个主链路做例子。
- 讲清入口、状态变化、数据写入、异步事件、失败补偿和观测指标。

### 生产验证指标

- 缺陷逃逸率
- 代码评审问题数
- 单元测试覆盖关键分支
- 兼容性破坏次数

### 低分陷阱

- 只背定义，不说明业务场景和失败场景。
- 只讲正常路径，不讲超时、重试、回滚、补偿和监控。
- 只给方案，不给验证指标和取舍边界。

### 30 秒高分收束

这道题我会用 Java 语言和工程基础 的视角回答。
先给结论，再给项目例子，然后补失败场景、验证指标和取舍边界。
这样能让面试官看到我不是只会背知识点，而是能把知识点落到生产系统。

## 架构取舍与反驳补强

架构取舍补强标记：已完成

### 先给立场

- 回答「为什么金额不能用 `double`？」时，不能只给单一方案，要先说明约束、目标和失败边界。
- 高分回答要让面试官看到你能在正确性、可用性、成本、复杂度和团队能力之间做判断。

### 可选方案对比

- 保守写法：团队理解成本低，但模板代码和误用风险可能更高。
- 现代 Java17 写法：表达力强，但需要规范约束避免炫技。
- 框架自动生成：效率高，但要避免隐藏业务不变量和兼容性风险。

### 反驳和防守

- 如果面试官问为什么不直接上最复杂方案，可以回答：复杂方案只有在规模和风险证明必要时才值得引入。
- 如果面试官问为什么不用最简单方案，可以回答：简单方案可以做第一期，但必须提前设计观测和迁移边界。
- 我的判断原则是：如果约束不明确，先补齐规模、延迟、可用性、一致性、成本和团队能力，再做选择。

### 决策证据

- 业务指标
- 稳定性指标
- 成本指标
- 灰度和回滚记录

### 一句话总结

我会先用简单可靠的方案解决当前确定性问题，同时保留观测、灰度和迁移能力。
当指标证明瓶颈存在，再演进到更复杂的架构，而不是为了显得高级提前复杂化。

## 生产落地验收补强

生产验收补强标记：已完成

### 上线前检查

- 针对「为什么金额不能用 `double`？」，先确认它影响的是正确性、稳定性、性能、安全还是成本。
- 确认代码规范、兼容性、边界校验和关键分支测试。
- 上线前用代码评审和静态检查避免新语法被滥用。

### 灰度和回滚

- 先在测试环境和影子流量中验证，再做 1%、5%、25%、50%、100% 分阶段灰度。
- 每个阶段都设置自动暂停条件和人工回滚负责人。
- 回滚不是只回代码，还要确认配置、数据、缓存、消息和任务状态能一起回到安全状态。

### 监控和验收证据

- 测试报告
- 灰度看板
- 告警规则
- 回滚记录

### 面试表达

我不会只说方案能实现，还会说明上线前怎么验收、上线中怎么看指标、出问题怎么回滚。
这能证明我关注的是长期稳定运行，而不是只完成一次功能开发。

## 规模化与成本治理补强

规模成本补强标记：已完成

### 规模化视角

- 回答「为什么金额不能用 `double`？」时，要主动放到 10 亿用户、1 亿 DAU、100W 峰值并发的背景下思考。
- 规模化时更关注代码可维护性、兼容性和错误预防。
- 语言特性要降低团队协作成本，而不是增加理解成本。

### 成本治理

- 用单位成本看问题，例如单请求成本、单订单成本、单消息成本和单 GB 存储成本。
- 先优化浪费最高的环节，而不是平均用力。

### 自动化和 owner

- 为关键指标建立看板、告警、owner 和 Runbook。
- 把经验沉淀成自动化检查、流水线门禁或平台能力。

### 面试表达

我会补一句：方案能跑只是第一步，大规模下还要回答容量怎么估、成本怎么控、故障谁负责。
这能体现我不是只会实现单点功能，而是能长期运营一个高并发业务系统。


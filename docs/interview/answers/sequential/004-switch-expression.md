# 004 `switch` 表达式相比传统 `switch` 有什么优势？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

`switch` 表达式相比传统 `switch` 有什么优势？

## 先给面试官的短答案

传统 `switch` 是语句，主要用于执行分支逻辑；新的 `switch` 表达式可以直接返回值，
语法更简洁，也减少忘记 `break` 导致的 fall-through 问题。

在业务系统里，它很适合表达有限状态的映射，例如订单状态到下一步动作、支付状态到展示文案、
库存预占状态到处理策略。

但我不会把复杂业务流程都塞进 `switch`。复杂状态机仍然需要领域方法、状态迁移校验、
审计、事件和补偿机制。

## 从零基础理解：传统 `switch` 的问题

传统写法：

```java
String action;
switch (order.status()) {
    case CREATED:
        action = "PAY";
        break;
    case PAID:
        action = "FULFILL";
        break;
    case CANCELLED:
    case CLOSED:
        action = "NONE";
        break;
    default:
        action = "RETRY";
        break;
}
```

问题：

- 代码啰嗦。
- 每个分支都要写 `break`。
- 忘记 `break` 会继续执行下一个分支。
- 变量要先声明，再在分支里赋值。
- 分支多时可读性下降。

新的 `switch` 表达式：

```java
String action = switch (order.status()) {
    case CREATED -> "PAY";
    case PAID -> "FULFILL";
    case CANCELLED, CLOSED -> "NONE";
    case PENDING_RETRY -> "RETRY";
};
```

它直接表达“根据状态得到动作”。

## 核心优势

### 减少 fall-through 错误

传统 `switch` 如果忘记 `break`，会继续执行后面的 case。
在订单、支付、库存状态处理中，这类错误可能导致严重业务问题。

`->` 写法默认不会 fall-through，更安全。

### 可以作为表达式返回值

`switch` 表达式可以直接赋值：

```java
HttpStatus httpStatus = switch (errorCode) {
    case NOT_FOUND -> HttpStatus.NOT_FOUND;
    case CONFLICT -> HttpStatus.CONFLICT;
    case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
    default -> HttpStatus.BAD_REQUEST;
};
```

这适合统一异常处理中错误码到 HTTP 状态码的映射。

### 分支更紧凑

多个状态可以写在同一分支：

```java
case CANCELLED, CLOSED -> "NONE";
```

比传统写法更清楚。

### 更适合枚举状态

电商系统有大量枚举：

- `OrderStatus`
- `PaymentStatus`
- `ReservationStatus`
- `OutboxStatus`
- `ErrorCode`

`switch` 表达式很适合这种有限集合。

## 在 eMall 项目中怎么用？

### 错误码映射

```java
HttpStatus status = switch (errorCode) {
    case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
    case FORBIDDEN -> HttpStatus.FORBIDDEN;
    case NOT_FOUND -> HttpStatus.NOT_FOUND;
    case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
    case DOWNSTREAM_UNAVAILABLE, SYSTEM_BUSY -> HttpStatus.SERVICE_UNAVAILABLE;
    case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    default -> HttpStatus.BAD_REQUEST;
};
```

这种映射逻辑短、稳定、清晰。

### 状态展示

```java
String display = switch (payment.status()) {
    case CREATED -> "Waiting for payment";
    case SUCCEEDED -> "Paid";
    case REFUNDING -> "Refunding";
    case REFUNDED -> "Refunded";
    case FAILED -> "Failed";
};
```

### 简单策略选择

```java
RetryPolicy policy = switch (event.status()) {
    case PENDING -> RetryPolicy.immediate();
    case FAILED -> RetryPolicy.exponentialBackoff();
    case PUBLISHED -> RetryPolicy.none();
};
```

## 什么时候不应该用？

### 复杂状态机

如果状态迁移包含：

- 权限校验。
- 数据库写入。
- 事件发布。
- 审计记录。
- 补偿状态。
- 下游调用。

就不应该只用一个大 `switch` 搞定。

例如订单支付不是简单：

```java
status = switch (status) {
    case CREATED -> PAID;
    default -> throw ...
};
```

真实逻辑还要确认库存、写订单、写 Outbox、处理失败补偿。

### 分支持续膨胀

如果 `switch` 有几十个 case，并且每个 case 都很多行，通常说明需要策略模式。

例如不同支付渠道的处理：

```text
ALIPAY -> ...
WECHAT -> ...
CARD -> ...
PAYPAL -> ...
```

这种更适合 `PaymentChannel` 接口加多个实现。

## 常见追问

### `switch` 表达式能不能抛异常？

可以。

```java
OrderAction action = switch (status) {
    case CREATED -> OrderAction.PAY;
    case PAID -> OrderAction.FULFILL;
    case CANCELLED -> OrderAction.NONE;
    default -> throw new BusinessException(ErrorCode.CONFLICT, "unsupported status");
};
```

### 它能不能替代策略模式？

不能完全替代。简单映射可以用 `switch`，复杂可扩展业务用策略模式。

判断标准：

- 分支稳定、逻辑短：用 `switch`。
- 分支经常扩展、逻辑复杂、需要独立测试：用策略模式。

### 专家级完整回答

```text
switch 表达式相比传统 switch 的优势是更安全、更简洁，可以直接返回值，
并且避免忘记 break 造成 fall-through。它很适合枚举状态映射，例如错误码到 HTTP 状态、
订单状态到展示动作、支付状态到处理策略。

但我不会把复杂业务流程都放进 switch。像订单支付、库存确认、支付回调这类流程，
涉及事务、状态机、Outbox、补偿和审计，应该由领域服务或应用服务承载。
switch 表达式适合表达简单决策，不适合替代完整业务建模。
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

- 能解释表达式和值返回。
- 能解释避免 fall-through。
- 能结合枚举状态和错误码映射。
- 能说明不适合复杂业务流程。
- 能说出和策略模式的边界。

## 二次深度补强

题目：`switch` 表达式相比传统 `switch` 有什么优势？

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

- 本题要围绕「`switch` 表达式相比传统 `switch` 有什么优势？」展开，不要只复述分类模板。
- 先解释 Java 语义，再说明它怎样降低电商项目中的误用成本。
- 重点补充边界条件、反例、代码规范和自动化测试。

### 专项图解说明

![逐题专项图解](../../assets/java-engineering-model.svg)

- 这张图用于把「`switch` 表达式相比传统 `switch` 有什么优势？」放回生产链路中理解，重点看入口、状态、数据和恢复闭环。
- 面试时可以先按图说明主路径，再补失败路径、监控指标和回滚手段。

### 贴合本题的实现示例

```java
public enum OrderStatus {
    CREATED, PAID, SHIPPED, CANCELLED
}

final class OrderStatusPolicy {

    boolean canCancel(OrderStatus status) {
        return switch (status) {
            case CREATED, PAID -> true;
            case SHIPPED, CANCELLED -> false;
        };
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
- 回答「`switch` 表达式相比传统 `switch` 有什么优势？」时，要从这些模块里选一个主链路做例子。
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

- 回答「`switch` 表达式相比传统 `switch` 有什么优势？」时，不能只给单一方案，要先说明约束、目标和失败边界。
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

- 针对「`switch` 表达式相比传统 `switch` 有什么优势？」，先确认它影响的是正确性、稳定性、性能、安全还是成本。
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

- 回答「`switch` 表达式相比传统 `switch` 有什么优势？」时，要主动放到 10 亿用户、1 亿 DAU、100W 峰值并发的背景下思考。
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


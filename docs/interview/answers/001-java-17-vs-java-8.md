# 001 Java 17 相比 Java 8 有哪些重要变化？

[返回按分类学习面试题](../README.md)

## 题目

Java 17 相比 Java 8 有哪些重要变化？

## 先给面试官的短答案

Java 8 是现代 Java 的重要起点，带来了 Lambda、Stream、Optional、默认方法和新的时间 API。
Java 17 是长期支持版本，在 Java 8 之后累计了很多语言、JVM、GC、安全和云原生运行时改进。

如果放到后端分布式服务里看，Java 17 的价值主要体现在四点：

- 代码表达力更强，例如 `record`、`var`、`switch` 表达式、文本块、sealed class。
- JVM 和 GC 更适合低延迟服务和容器化部署。
- 标准库、安全能力和运行时性能持续增强。
- 作为 LTS 版本，更适合企业生产长期维护。

面试里不要只背“新增了 record”。更好的回答是：

```text
Java 17 相比 Java 8，不只是语法更新，而是让 Java 更适合现代云原生后端服务。
我会用 record 表达不可变 DTO 和事件，用 switch 表达状态机分支，
用更好的容器感知和 GC 能力支撑 Kubernetes 部署。
但我也会控制新语法使用范围，避免团队可读性下降。
```

## 从零基础理解：为什么版本变化重要？

Java 是一种语言，也是一套运行平台。写 Java 程序时，你写的是 `.java` 文件；
编译后会变成 `.class` 字节码；真正运行代码的是 JVM。

所以 Java 版本升级通常包含三类变化：

- 语言变化：让你写代码更简单、更安全、更清晰。
- 标准库变化：提供更多官方工具类和 API。
- JVM 变化：让程序运行得更快、更稳定、更适合现代部署环境。

Java 8 到 Java 17 跨了很多版本。不是每个变化面试都要背，但要知道哪些变化对后端工程有实际价值。

## Java 8 的核心价值

理解 Java 17 前，要先知道 Java 8 为什么重要。

Java 8 带来的核心能力包括：

- Lambda 表达式。
- Stream API。
- `Optional`。
- 接口默认方法。
- 新时间 API，例如 `Instant`、`LocalDateTime`、`Duration`。

这些能力让 Java 从传统面向对象写法，逐步支持更简洁的函数式风格。

例如没有 Lambda 时，排序可能这样写：

```java
orders.sort(new Comparator<Order>() {
    @Override
    public int compare(Order left, Order right) {
        return left.createdAt().compareTo(right.createdAt());
    }
});
```

有 Lambda 后可以这样写：

```java
orders.sort(Comparator.comparing(Order::createdAt));
```

这不是单纯少写代码，而是让业务意图更明显：按订单创建时间排序。

## Java 17 的语言层面变化

### `record`

`record` 用来表达不可变数据载体。它自动生成构造函数、访问器、`equals`、`hashCode`
和 `toString`。

适合电商系统中的 DTO、事件和查询结果：

```java
public record CreateOrderRequest(
        String requestId,
        long userId,
        long skuId,
        int quantity
) {
}
```

相比传统类：

```java
public class CreateOrderRequest {
    private final String requestId;
    private final long userId;
    private final long skuId;
    private final int quantity;

    public CreateOrderRequest(String requestId, long userId, long skuId, int quantity) {
        this.requestId = requestId;
        this.userId = userId;
        this.skuId = skuId;
        this.quantity = quantity;
    }

    public String requestId() {
        return requestId;
    }
}
```

`record` 能减少模板代码，让人更关注业务字段。

但专家级回答要补一句限制：

```text
record 适合不可变边界对象，不适合复杂生命周期的领域聚合。
比如下单请求可以用 record，但订单状态机是否用 record 要看业务设计。
```

### `var`

`var` 是局部变量类型推断。它不会让 Java 变成动态语言，变量类型仍然在编译期确定。

适合：

```java
var order = orderService.get(orderId);
var events = outboxRepository.findReadyToPublish(100);
```

不适合：

```java
var data = client.call(input);
```

如果右侧方法名和变量名都不清楚，`var` 会降低可读性。

面试表达：

```text
我会把 var 当作降低噪音的工具，而不是炫技。
团队里应该约定只在类型明显的局部变量中使用，公共 API 不使用 var。
```

### `switch` 表达式

传统 `switch` 容易漏写 `break`。Java 17 的 `switch` 表达式可以直接返回值。

```java
String action = switch (order.status()) {
    case CREATED -> "PAY";
    case PAID -> "FULFILL";
    case CANCELLED, CLOSED -> "NONE";
    case PENDING_RETRY -> "RETRY";
};
```

这很适合表达订单状态、支付状态、库存状态这类有限状态映射。

注意：复杂状态机不要全塞进一个 `switch`，应该有明确的领域方法、校验、审计和事件。

### 文本块

文本块让多行字符串更容易读，适合 SQL、JSON、日志模板。

```java
String sql = """
        update inventory_item
        set available = available - ?,
            reserved = reserved + ?
        where sku_id = ?
          and available >= ?
        """;
```

在分布式服务中，很多关键逻辑最终会落到 SQL、配置、JSON payload。
文本块能提升可读性，减少拼接错误。

### sealed class

sealed class 可以限制哪些类能继承它。它适合表达有限类型集合。

例如风控决策：

```java
public sealed interface RiskDecision
        permits RiskDecision.Pass, RiskDecision.Reject, RiskDecision.ManualReview {

    record Pass(String reason) implements RiskDecision {
    }

    record Reject(String reason) implements RiskDecision {
    }

    record ManualReview(String reason) implements RiskDecision {
    }
}
```

这样风控结果只能是通过、拒绝、人工审核三种，不会被随意扩展。

专家级表达：

```text
sealed class 的价值在于建模边界。对支付结果、风控决策、运维操作结果这类有限集合，
它能让编译器帮助我们约束业务类型。
```

## JVM 和运行时变化

Java 17 不只是语法更好，JVM 也有很多长期演进。

### 更好的 GC 选择

Java 8 常见生产选择是 Parallel GC、CMS、G1。Java 17 中 G1 更成熟，
同时还有 ZGC、Shenandoah 等面向低延迟场景的 GC。

对电商系统来说，GC 重要是因为：

- 下单接口需要低延迟。
- 支付回调不能长时间停顿。
- 网关和核心服务 P99 受 GC 暂停影响明显。
- 大促期间对象分配更多，GC 行为会影响稳定性。

面试时可以这样说：

```text
Java 17 提供了更成熟的低延迟 GC 选择，但我不会盲目切换。
我会先通过 GC 日志、P99、吞吐、CPU 和内存指标定位瓶颈，
再根据服务是吞吐优先还是延迟优先选择 GC。
```

### 更好的容器支持

现代 Java 服务通常部署在 Kubernetes 中。容器有 CPU 和内存限制。
早期 Java 版本对容器限制感知不完善，可能把宿主机资源误认为自己可用资源。

Java 17 对容器环境支持更成熟，能更好识别 cgroup 限制。

这对生产很重要：

- 避免 JVM 堆设置超过容器限制导致 OOMKilled。
- 避免线程数、GC 线程数和 CPU 限额不匹配。
- 更容易在 K8s 中设置 requests、limits 和 JVM 参数。

专家级表达：

```text
Java 17 对云原生部署更友好。部署到 Kubernetes 时，我会同时关注容器 memory limit、
JVM MaxRAMPercentage、堆外内存、线程栈和直接内存，而不是只看 -Xmx。
```

## 安全和维护层面变化

Java 17 是 LTS 版本。LTS 的意思是长期支持，更适合生产系统。

生产系统选择 Java 版本时，不只看语法，还要看：

- 安全补丁。
- 依赖生态支持。
- 框架兼容性。
- 运维工具支持。
- 团队熟悉度。
- 长期维护成本。

很多现代框架已经把 Java 17 作为推荐或最低版本之一，例如新版本 Spring Boot。
如果项目还停留在 Java 8，可能遇到依赖版本受限、安全补丁压力和性能优化缺失。

## 放到 eMall 项目里怎么讲？

在 eMall 这类 Java 17 电商系统里，可以这样关联：

- DTO 和事件 payload 可以使用 `record`，让数据不可变。
- 订单、支付、库存状态映射可以使用 `switch` 表达式。
- SQL 条件更新和复杂 JSON 示例可以用文本块提升可读性。
- Kubernetes 部署时利用 Java 17 更好的容器感知能力。
- JVM 指标、GC 日志和 P99 延迟用于验证运行时效果。

示例回答：

```text
在 eMall 项目中，我不会为了使用新语法而使用新语法。
例如下单请求、支付回调请求、Outbox 事件 payload 适合用 record；
订单状态转换可以用 switch 表达式辅助表达；
库存条件扣减 SQL 可以用文本块提高可读性。
同时 Java 17 作为 LTS，更适合和 Spring Boot 3、Kubernetes、现代 GC 搭配。
```

## 常见追问

### Java 17 一定比 Java 8 性能更好吗？

不一定。大多数情况下 Java 17 的 JVM 优化更好，但真实性能取决于代码、GC、配置、硬件、
依赖库和业务负载。

专家回答：

```text
我不会只因为版本升级就宣称性能提升。升级后要通过压测、GC 日志、P95/P99、CPU、
内存和错误率对比验证。版本升级是手段，不是结论。
```

### Java 17 新语法会不会增加团队学习成本？

会，所以要约束使用范围。

建议：

- `record` 用于 DTO、事件、不可变值对象。
- `var` 只用于类型明显的局部变量。
- `switch` 表达式用于简单状态映射。
- sealed class 用于有限类型建模。
- 团队代码规范和 Code Review 统一约束。

### 从 Java 8 升级到 Java 17 有哪些风险？

常见风险：

- 依赖库不兼容。
- 反射访问 JDK 内部 API 失败。
- JVM 参数变化。
- GC 默认行为变化。
- 日期、编码、TLS、安全策略差异。
- 构建工具和插件版本过旧。

升级路径：

1. 先升级 Maven、插件和核心依赖。
2. 本地编译和单元测试。
3. 集成测试覆盖数据库、Redis、Kafka、HTTP 客户端。
4. 压测对比 P95/P99、CPU、内存、GC。
5. 小流量灰度。
6. 观察错误率和核心业务指标。
7. 分批放量。

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

## 专家级完整回答模板

面试时可以这样回答：

```text
Java 17 相比 Java 8 的变化可以从语言、JVM 和生产维护三个层面看。

语言层面，Java 8 带来了 Lambda、Stream、Optional 和新时间 API；
Java 17 进一步提供 record、var、switch 表达式、文本块和 sealed class。
这些能力能减少模板代码，更好表达不可变 DTO、事件、状态机和有限业务类型。

JVM 层面，Java 17 的 GC、性能和容器支持比 Java 8 更成熟，
更适合 Kubernetes 部署和低延迟服务。但我不会只因为升级就认为性能一定提升，
需要通过 GC 日志、P99、CPU、内存和压测数据验证。

生产维护层面，Java 17 是 LTS，生态和安全支持更适合长期维护。
在电商系统里，我会用 record 表达下单请求和事件 payload，
用 switch 表达订单状态映射，用文本块写复杂 SQL，
同时通过团队规范限制新语法滥用，保证代码可读性和可维护性。
```

## 回答评分点

高分答案应该覆盖：

- 能说出 Java 8 和 Java 17 的关键变化。
- 不只背语法，而能讲工程价值。
- 能关联分布式服务和电商系统。
- 能讲 JVM、GC、容器支持和 LTS。
- 能主动说明风险、约束和升级验证方式。

低分答案通常是：

- 只说 “Java 17 多了 record”。
- 只背特性，不知道用在哪里。
- 不知道 LTS、GC、容器部署的意义。
- 不知道升级风险。
- 不能结合项目说明。

## 深度完善：面向 L6 的回答框架

围绕「Java 17 相比 Java 8 有哪些重要变化？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：Java 17 相比 Java 8 有哪些重要变化？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

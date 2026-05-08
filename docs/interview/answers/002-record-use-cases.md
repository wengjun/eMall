# 002 `record` 适合哪些场景，不适合哪些场景？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

`record` 适合哪些场景，不适合哪些场景？

## 先给面试官的短答案

`record` 是 Java 用来表达“不可变数据载体”的语法。它适合 DTO、值对象、查询结果、
事件 payload、配置快照这类“创建后不应该再变”的对象。

它不适合复杂领域聚合、需要频繁修改状态的对象、需要框架代理的实体、需要复杂继承层次的类型。

面试里可以这样回答：

```text
我会把 record 用在边界对象和不可变值对象上，例如下单请求、支付回调请求、Outbox 事件 payload。
它能减少 getter、equals、hashCode 这类模板代码，让数据结构更清晰。
但我不会盲目把所有 domain 都改成 record。像订单状态机、库存预占、支付单这类有复杂生命周期的对象，
是否使用 record 要看状态变化方式、ORM 映射方式和领域行为封装。
```

## 从零基础理解：`record` 是什么？

在 Java 里，我们经常需要写一些只用来装数据的类。例如一个创建订单请求：

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

    public long userId() {
        return userId;
    }

    public long skuId() {
        return skuId;
    }

    public int quantity() {
        return quantity;
    }
}
```

这个类没有复杂行为，只是保存四个字段。传统写法很啰嗦。

用 `record` 可以写成：

```java
public record CreateOrderRequest(
        String requestId,
        long userId,
        long skuId,
        int quantity
) {
}
```

Java 编译器会自动生成：

- 构造函数。
- 字段访问方法，例如 `requestId()`、`userId()`。
- `equals`。
- `hashCode`。
- `toString`。

所以 `record` 的本质不是“更短的 class”，而是明确告诉读代码的人：

```text
这个类型主要用来表达一组不可变数据。
```

## `record` 的核心特征

### 字段默认是 private final

`record` 的组件创建后不能重新赋值。

```java
CreateOrderRequest request = new CreateOrderRequest("req-1", 10001L, 20001L, 1);
```

你不能再写：

```java
request.quantity = 2;
```

这对分布式系统很重要。下单请求、支付回调、事件消息一旦进入系统，就应该作为事实输入保存，
不应该在多个方法之间传递时被悄悄改掉。

### 自动实现相等判断

两个 `record` 如果字段值都相同，`equals` 通常就相等。

```java
record SkuKey(long skuId, int bucketNo) {
}

SkuKey left = new SkuKey(10001L, 3);
SkuKey right = new SkuKey(10001L, 3);

System.out.println(left.equals(right)); // true
```

这让 `record` 很适合作为简单 key。

### 不是完全深度不可变

这是容易被忽略的点。`record` 的字段引用不可变，但字段指向的对象不一定不可变。

```java
public record CartSnapshot(List<Long> skuIds) {
}
```

如果外部传入的是可变 `ArrayList`，调用方仍然可能修改这个 List。

更安全的写法：

```java
public record CartSnapshot(List<Long> skuIds) {
    public CartSnapshot {
        skuIds = List.copyOf(skuIds);
    }
}
```

专家级回答要主动说出这一点：

```text
record 是浅不可变，不是自动深不可变。包含集合时要做防御性拷贝。
```

## 适合使用 `record` 的场景

### HTTP 请求 DTO

DTO 是 Data Transfer Object，用来在系统边界传输数据。

上面创建订单请求的例子就是典型 HTTP 请求 DTO。

适合原因：

- 请求进入系统后不应该被修改。
- 字段就是接口契约。
- 代码简洁。
- 方便测试构造。

但是参数校验仍然要做：

```java
public record CreateOrderRequest(
        @NotBlank String requestId,
        @Positive long userId,
        @Positive long skuId,
        @Positive int quantity
) {
}
```

### HTTP 响应 DTO

响应对象也适合 `record`：

```java
public record OrderResponse(
        long orderId,
        String status,
        BigDecimal payableAmount,
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.orderId(),
                order.status().name(),
                order.payableAmount(),
                order.createdAt());
    }
}
```

好处是外部响应和内部领域对象分开。你可以控制哪些字段返回给前端，避免把内部字段、
失败原因、审计字段、敏感字段泄露出去。

### MQ 事件 payload

分布式系统中，服务之间经常通过消息传递业务事实。

```java
public record OrderPaidEvent(
        long orderId,
        long userId,
        long skuId,
        int quantity,
        BigDecimal paidAmount,
        Instant paidAt
) {
}
```

事件天然适合不可变，因为事件表示“已经发生的事实”。

如果一个订单已支付事件发出后，又被某段代码修改了金额或用户 ID，这在审计和排障中会非常危险。

专家级表达：

```text
事件 payload 我倾向于不可变，因为事件是事实记录。
不可变能减少消费者之间共享对象导致的数据污染，也更适合审计和重放。
```

### 查询投影结果

查询接口经常不需要完整领域对象，只需要部分字段。

```java
public record UserOrderSummary(
        long orderId,
        String status,
        BigDecimal amount,
        Instant createdAt
) {
}
```

这种对象不负责业务状态变更，只负责承载查询结果，很适合 `record`。

### 值对象

值对象是通过值判断相等的对象，不关心身份 ID。

例如库存桶 key：

```java
public record InventoryBucketKey(long skuId, int bucketNo) {
}
```

例如金额和币种：

```java
public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
    }
}
```

注意：金额值对象要小心 `BigDecimal` scale，比较时通常使用 `compareTo`。

### 配置快照

如果某段逻辑需要读取一组配置并在执行期间保持不变，可以用 `record` 表达配置快照：

```java
public record RateLimitPolicy(
        int permitsPerSecond,
        int burstCapacity,
        Duration timeout
) {
}
```

这样业务代码读到的是一个明确、不可变的策略对象。

## 不适合使用 `record` 的场景

### 复杂生命周期的领域聚合

订单、支付单、库存预占这类对象有复杂状态迁移。

例如订单可能经历：

```text
CREATED -> PAID -> FULFILLING -> COMPLETED
CREATED -> CANCELLED
PENDING_RETRY -> CREATED
```

如果用 `record`，每次状态变化都要创建新对象。这不是一定不行，但需要团队明确采用不可变领域模型。
如果团队习惯 ORM 实体可变模型，盲目使用 `record` 会增加理解成本。

更重要的是，领域聚合不只是字段集合，还要保护业务不变量。

例如：

```java
public Order markPaid() {
    if (status != OrderStatus.CREATED) {
        throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid");
    }
    return new Order(..., OrderStatus.PAID, ...);
}
```

是否用 `record` 不是关键，关键是状态变化必须通过明确业务方法。

### 需要 JPA 代理的实体

很多 ORM 框架，尤其 JPA/Hibernate，依赖无参构造、字段代理、延迟加载和可变实体。
`record` 默认 final、字段 final，不适合这种模式。

如果项目使用 MyBatis、Spring JDBC 或手写映射，`record` 用于查询结果会更自然。
但如果使用 JPA 做实体映射，通常不建议直接把 Entity 设计成 `record`。

专家级表达：

```text
record 和 ORM 的匹配度取决于持久化技术。它很适合 JDBC/MyBatis 的投影结果，
但不适合需要代理和脏检查的 JPA Entity。
```

### 需要复杂继承层次的对象

`record` 隐式继承 `java.lang.Record`，不能再继承其他类。
它可以实现接口，但不能作为复杂继承层次的一部分。

如果业务需要多态行为，通常用接口加实现类，或者 sealed interface 加 record 实现。

### 包含大量可变集合且不做保护的对象

下面这个写法看起来不可变，实际不安全：

```java
public record PromotionResult(List<Coupon> coupons) {
}
```

如果 `coupons` 是外部可变 List，创建后仍然可能被修改。

安全写法：

```java
public record PromotionResult(List<Coupon> coupons) {
    public PromotionResult {
        coupons = List.copyOf(coupons);
    }
}
```

如果集合元素 `Coupon` 本身也是可变对象，还要考虑元素是否需要不可变。

### 需要隐藏大量内部计算缓存的对象

`record` 的组件就是它的主要状态。如果一个对象有很多内部缓存、懒加载字段、复杂派生状态，
使用普通 class 往往更清晰。

## 和 Lombok `@Data` 有什么区别？

很多 Java 项目使用 Lombok `@Data` 自动生成 getter、setter、`equals`、`hashCode`、`toString`。

`@Data` 和 `record` 的核心区别：

| 对比项 | `record` | Lombok `@Data` |
| --- | --- | --- |
| 是否 Java 标准 | 是 | 否，需要 Lombok |
| 默认是否不可变 | 是，组件 final | 否，通常生成 setter |
| 是否适合 DTO | 适合不可变 DTO | 适合可变 JavaBean |
| 是否需要 IDE 插件 | 不需要 | 通常需要 |
| 是否适合 JPA Entity | 通常不适合 | 更常见，但要谨慎 |

Lombok `@Data` 的风险：

- 自动生成 setter，可能破坏业务不变量。
- 自动生成 `equals/hashCode`，可能把不该参与比较的字段放进去。
- `toString` 可能打印敏感信息或循环引用。

专家级表达：

```text
如果是不可变 DTO 或事件，我优先考虑 record。
如果框架要求 JavaBean 风格，才考虑 Lombok，但不会无脑使用 @Data，
尤其是领域对象和实体对象要谨慎生成 setter、equals、hashCode。
```

## 在 eMall 项目中怎么用？

eMall 是 Java 17 电商系统，`record` 可以用于：

- 下单请求 DTO。
- 支付回调请求 DTO。
- 库存预占请求和响应。
- Outbox 事件 payload。
- 运维操作结果。
- 查询投影对象。
- 测试用例中的输入输出对象。

例如库存预占结果：

```java
public record InventoryReservationResult(
        String requestId,
        long skuId,
        int quantity,
        boolean reserved,
        String reason
) {
}
```

这个对象表达库存服务对订单服务的返回结果。它不应该在订单服务里被修改。

不建议用 `record` 的地方：

- 如果某个 ORM Entity 需要无参构造和可变字段。
- 如果对象状态变化非常复杂且团队没有采用不可变领域建模。
- 如果对象包含敏感字段且 `toString` 可能被日志打印。

注意最后一点：`record` 自动生成 `toString`，如果字段包含手机号、token、地址、密钥，
日志里可能泄露敏感信息。敏感对象要避免直接使用默认 `toString`，或者重写。

## 常见追问

### `record` 是不是就不需要写校验？

不是。`record` 只帮你生成模板代码，不会自动保证业务合法。

可以在 compact constructor 里做基础校验：

```java
public record CreateOrderRequest(String requestId, long userId, long skuId, int quantity) {
    public CreateOrderRequest {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
```

但要区分参数校验和业务校验：

- 参数校验：数量必须大于 0。
- 业务校验：库存是否足够、用户是否被冻结、商品是否上架。

后者应该放在业务服务中。

### `record` 能不能有方法？

可以。`record` 可以定义方法。

```java
public record Money(BigDecimal amount, String currency) {
    public boolean isPositive() {
        return amount.signum() > 0;
    }
}
```

但如果方法越来越多、状态变化越来越复杂，说明它可能不只是数据载体了，需要重新评估是否用普通 class。

### `record` 能不能实现接口？

可以。

```java
public interface DomainEvent {
    String eventType();
}

public record OrderPaidEvent(long orderId, Instant paidAt) implements DomainEvent {
    @Override
    public String eventType() {
        return "ORDER_PAID";
    }
}
```

这很适合事件系统。

### `record` 是否线程安全？

不能简单说一定线程安全。

如果所有字段都是不可变对象，那么它天然更接近线程安全。
如果字段里包含可变集合、可变对象，那么仍然可能不安全。

例如：

```java
public record UnsafeSnapshot(List<String> values) {
}
```

这个不是深度线程安全，因为 `values` 可能被修改。

### `record` 会不会影响序列化？

现代 Jackson 等库通常支持 `record`，但要确认版本。
如果项目依赖较老框架，可能需要升级序列化库。

生产系统中还要注意：

- 字段名就是 JSON 契约的一部分。
- 改 record 组件名可能破坏 API。
- 新增字段要考虑老客户端兼容。
- MQ 事件 payload 改字段要考虑老消费者兼容。

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
record 是 Java 里表达不可变数据载体的标准语法。它会自动生成构造函数、访问器、
equals、hashCode 和 toString，适合 DTO、值对象、查询投影和事件 payload。

在分布式电商系统里，我会把下单请求、支付回调请求、库存预占结果、Outbox 事件 payload
设计成 record，因为这些对象创建后不应该被修改，且它们主要表达数据流。

但我不会把所有类都改成 record。对于订单、库存、支付这类有复杂生命周期和状态迁移的领域对象，
重点是保护业务不变量，是否用 record 要看持久化方式和建模方式。
如果使用 JPA 这类依赖代理和无参构造的 ORM，record 通常不适合做 Entity。

另外 record 是浅不可变。如果字段里有 List、Map 或可变对象，需要做防御性拷贝。
还要注意默认 toString 可能打印敏感字段，所以包含手机号、token、密钥的对象不能无脑使用默认输出。
```

## 回答评分点

高分答案应该覆盖：

- 能说明 `record` 是不可变数据载体。
- 能说出适合 DTO、事件、值对象、查询结果。
- 能说出不适合复杂生命周期领域对象和 JPA Entity。
- 能指出浅不可变问题和集合防御性拷贝。
- 能比较 `record` 和 Lombok `@Data`。
- 能关联分布式系统中的事件、幂等、审计和日志安全。

低分答案通常是：

- 只说 “record 可以少写 getter/setter”。
- 不知道 `record` 默认不可变。
- 不知道集合字段仍然可能可变。
- 不知道它和 ORM、Lombok 的区别。
- 不知道默认 `toString` 的敏感信息风险。

## 深度完善：面向 L6 的回答框架

围绕「`record` 适合哪些场景，不适合哪些场景？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：`record` 适合哪些场景，不适合哪些场景？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

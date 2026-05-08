# 014 组合和继承如何取舍？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

组合和继承如何取舍？

## 先给面试官的短答案

继承表达 “is-a”，组合表达 “has-a” 或 “uses-a”。业务系统里我会优先使用组合，
只有在类型层次稳定、确实存在 is-a 关系时才使用继承。

原因是继承会把父类和子类强耦合，父类变化容易影响所有子类；组合依赖更明确，
更容易测试、替换和演进。

## 从零基础理解

继承：

```java
class Cat extends Animal {
}
```

表示 Cat 是一种 Animal。

组合：

```java
class OrderService {
    private final InventoryClient inventoryClient;
}
```

表示 OrderService 使用 InventoryClient。

业务系统中，大多数关系其实是“使用能力”，不是“是某种类型”。

## 为什么优先组合？

### 依赖更明确

```java
public class OrderService {
    private final PricingClient pricingClient;
    private final MarketingClient marketingClient;
    private final InventoryClient inventoryClient;
}
```

看构造函数就知道订单服务依赖价格、营销、库存。

### 更容易测试

测试时可以替换依赖：

```java
OrderService service = new OrderService(
        fakeOrderRepository,
        fakeOutboxRepository,
        idGenerator,
        fakeInventoryClient,
        fakePricingClient,
        fakeMarketingClient);
```

如果通过继承父类隐藏依赖，测试会更困难。

### 更容易替换实现

库存客户端可以从 HTTP 实现换成 MQ、Mock、内存实现，只要接口不变即可。

### 避免父类膨胀

很多项目喜欢写：

```java
class BaseService {
    // logging, cache, transaction, validation, metrics, utils...
}
```

最后所有服务都继承一个巨大父类，变成强耦合。

## 什么时候可以用继承？

继承适合：

- 类型层次非常稳定。
- 子类确实是父类的一种。
- 父类定义通用行为，子类只是特化。
- 不会形成很深的继承链。

例如异常体系：

```java
public class BusinessException extends RuntimeException {
}
```

这里业务异常是一种运行时异常，使用继承合理。

## 什么时候不要用继承？

不要为了复用几行代码就继承。

坏例子：

```java
class PaymentService extends OrderService {
}
```

支付服务不是订单服务的一种，它们只是协作关系。

更好的方式：

```java
class PaymentService {
    private final OrderClient orderClient;
}
```

## 设计模式中的体现

策略模式就是组合优先的例子：

```java
public interface PaymentChannel {
    PaymentResult pay(PaymentCommand command);
}
```

支付服务组合多个支付渠道：

```java
private final Map<String, PaymentChannel> channels;
```

新增渠道时新增实现类，不改支付服务主体逻辑。

## 在 eMall 项目中怎么讲？

eMall 中订单服务不继承库存服务、价格服务、营销服务，而是组合客户端：

```text
OrderService uses PricingClient
OrderService uses MarketingClient
OrderService uses InventoryClient
```

这是更合理的依赖关系。

专家级表达：

```text
我会优先组合，因为微服务和业务模块更需要清晰依赖边界。
继承适合稳定类型层次，组合适合能力复用和服务协作。
大型系统里继承滥用会造成隐式耦合，组合更利于测试、替换和演进。
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

- is-a 和 has-a/uses-a 区别。
- 业务系统优先组合。
- 继承适合稳定类型层次。
- 能说明测试、替换、耦合影响。
- 能结合 OrderService 组合下游 Client。

## 深度完善：面向 L6 的回答框架

围绕「组合和继承如何取舍？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：组合和继承如何取舍？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

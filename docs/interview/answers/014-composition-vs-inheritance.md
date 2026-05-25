# 014 组合和继承如何取舍？

[返回按分类学习面试题](../README.md)

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

## 回答评分点

高分答案应该覆盖：

- is-a 和 has-a/uses-a 区别。
- 业务系统优先组合。
- 继承适合稳定类型层次。
- 能说明测试、替换、耦合影响。
- 能结合 OrderService 组合下游 Client。

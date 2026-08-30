# 118 构造函数注入、字段注入、Setter 注入如何取舍？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

必需依赖优先用构造函数注入，能保证对象创建后依赖完整，也更利于测试和不可变设计。
可选依赖或运行期可变依赖可以用 Setter 注入。字段注入不推荐，因为隐藏依赖、难测试、无法使用 final，
也不利于发现循环依赖。

生产代码默认选择构造函数注入。

## 构造函数注入

示例：

```java
public OrderService(OrderRepository repository, PaymentClient paymentClient) {
    this.repository = repository;
    this.paymentClient = paymentClient;
}
```

优点：

- 依赖显式。
- 支持 `final`。
- 对象创建后完整。
- 单元测试方便。
- 循环依赖更早暴露。

适合必需依赖。

## 字段注入

示例：

```java
@Autowired
private OrderRepository repository;
```

问题：

- 依赖隐藏。
- 不能声明 final。
- 单元测试不方便。
- 容易让类依赖过多。
- 循环依赖可能被掩盖。

因此不推荐在生产业务代码中使用。

## Setter 注入

示例：

```java
public void setNotifier(Notifier notifier) {
    this.notifier = notifier;
}
```

适合：

- 可选依赖。
- 运行期可变依赖。
- 框架扩展点。

不适合作为必需依赖默认方式。

## 判断标准

取舍规则：

- 必需依赖：构造函数注入。
- 可选依赖：Setter 注入。
- 配置属性：构造函数绑定或配置类。
- 测试替换：构造函数最方便。
- 字段注入：尽量避免。

如果构造函数参数太多，说明类职责可能过重。

## 电商系统实践

订单服务依赖订单仓储、库存客户端、支付客户端，这些是必需依赖，应使用构造函数注入。

可选的审计通知器或实验开关，可以用 Setter 或配置组合。

如果一个 Service 构造函数有十几个依赖，要考虑拆分职责，而不是改回字段注入。

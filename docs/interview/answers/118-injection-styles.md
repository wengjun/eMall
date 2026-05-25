# 118 构造函数注入、字段注入、Setter 注入如何取舍？

[返回按分类学习面试题](../README.md)

## 题目

构造函数注入、字段注入、Setter 注入如何取舍？

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

## 在 eMall 项目中怎么讲？

订单服务依赖订单仓储、库存客户端、支付客户端，这些是必需依赖，应使用构造函数注入。

可选的审计通知器或实验开关，可以用 Setter 或配置组合。

如果一个 Service 构造函数有十几个依赖，要考虑拆分职责，而不是改回字段注入。

## 深度增强：Spring 服务治理图

![Spring 微服务调用栈和治理边界](../assets/spring-service-stack.svg)

Spring 题要从框架机制讲到业务边界。Controller 负责协议适配，Service 负责业务事务，Repository 负责数据访问；
事务、AOP、校验、错误码、配置和观测都是为了让微服务在复杂调用中保持稳定。

## 深度增强：Java 17 分层示例

```java
record CreateOrderCommand(long userId, long skuId, int quantity) {
}

record CreateOrderResult(long orderId, String status) {
}

interface OrderApplicationService {
    CreateOrderResult create(CreateOrderCommand command);
}

final class OrderControllerAdapter {
    private final OrderApplicationService service;

    OrderControllerAdapter(OrderApplicationService service) {
        this.service = service;
    }

    CreateOrderResult submit(CreateOrderCommand command) {
        return service.create(command);
    }
}
```

这个示例表达分层边界：接口层不堆业务逻辑，业务层不依赖 Web 协议，命令和结果对象形成稳定契约。

## 深度增强：生产边界

框架默认值不能替代设计。事务传播、异常回滚、异步线程池、连接池、序列化、超时和重试都要显式治理。
尤其在订单、支付、库存链路中，要避免长事务、隐式重试和跨服务事务误用。

## 深度增强：面试高分表达

我会先解释框架原理，再说明在电商系统里怎么落地。高分回答要能把自动配置、AOP、事务、MVC、WebFlux、
校验和错误处理，连接到可维护性、可观测性、稳定性和故障恢复。

## 专家级完整回答

```text
我默认使用构造函数注入，因为它让依赖显式、支持 final、对象创建后状态完整，并且便于单元测试。
Setter 注入适合可选或可变依赖。字段注入隐藏依赖、难测试、不能 final，也容易掩盖循环依赖，
生产代码不推荐。

构造函数参数过多是设计信号，通常说明类职责太大，需要拆分，而不是换一种注入方式隐藏问题。
```

## 回答评分点

高分答案应该覆盖：

- 必需依赖用构造函数。
- 字段注入不推荐。
- Setter 适合可选依赖。
- 构造函数参数过多是职责问题。
- 能联系测试和不可变。

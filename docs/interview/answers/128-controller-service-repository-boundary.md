# 128 Controller、Service、Repository 的职责边界是什么？

[返回按分类学习面试题](../README.md)

## 题目

Controller、Service、Repository 的职责边界是什么？

## 先给面试官的短答案

Controller 负责协议适配和参数校验，Service 负责编排业务用例和事务边界，Repository 负责数据访问和持久化抽象。
Controller 不应写业务规则，Repository 不应编排业务流程，Service 不应堆成上帝类。

清晰边界能提高可测试性、可维护性和模块演进能力。

## Controller

Controller 职责：

- 接收 HTTP 请求。
- 解析参数。
- 基础格式校验。
- 调用应用服务。
- 转换响应。
- 处理协议状态码。

不应该：

- 写复杂业务逻辑。
- 直接操作数据库。
- 调用多个 Repository 编排事务。

## Service

Service 职责：

- 表达业务用例。
- 编排领域对象和外部依赖。
- 控制事务边界。
- 做权限和业务校验。
- 发布领域事件或应用事件。

Service 应该面向业务动作，例如 `createOrder`、`payOrder`、`cancelOrder`。

## Repository

Repository 职责：

- 封装数据访问。
- 隐藏 SQL 或 ORM 细节。
- 提供领域语义查询。
- 保存和加载聚合或实体。

不应该：

- 调用远程服务。
- 编排业务流程。
- 处理 HTTP 参数。

## DTO 和领域对象

Controller 接收的是 request DTO。

Service 使用命令对象或领域对象。

Repository 返回实体或持久化对象。

不要让前端 DTO 穿透到所有层，否则协议变化会污染业务层。

## 在 eMall 项目中怎么讲？

订单创建：

- Controller 接收 `CreateOrderRequest`。
- Service 执行创建订单用例，校验用户、库存、价格和幂等。
- Repository 保存订单和查询订单。

如果 Controller 直接扣库存、算价格和写订单，就会变成胖 Controller，难测试也难复用。

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
Controller 是协议适配层，负责 HTTP 参数、基础校验和响应转换；Service 是应用用例层，负责编排业务、
事务边界和调用领域能力；Repository 是数据访问抽象，负责持久化和查询。DTO 不应随意穿透所有层。

边界清晰后，Controller 可以薄，Service 可以按用例测试，Repository 可以替换实现，系统更容易演进。
```

## 回答评分点

高分答案应该覆盖：

- Controller 做协议适配。
- Service 做业务用例和事务。
- Repository 做数据访问。
- 防止胖 Controller 和上帝 Service。
- DTO 不应污染所有层。

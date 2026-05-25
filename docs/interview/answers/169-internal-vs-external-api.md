# 169 如何设计内部 API 和外部 API 的边界？

[返回按分类学习面试题](../README.md)

## 题目

如何设计内部 API 和外部 API 的边界？

## 先给面试官的短答案

外部 API 面向合作方或客户端，必须稳定、安全、兼容、限流、签名和文档完善；内部 API 面向服务间调用，
可以更贴近内部模型，但仍要有契约、认证、超时、幂等和版本治理。外部 API 不应直接暴露内部领域模型和数据库结构。

边界原则是外部稳定受控，内部高效但不随意。

## 外部 API 特点

外部 API 要求：

- 长期兼容。
- 签名验签。
- 防重放。
- 权限控制。
- 限流配额。
- 审计。
- 完整文档。
- 明确 SLA。

外部调用方升级不可控，所以不能频繁破坏契约。

## 内部 API 特点

内部 API 也要治理：

- 服务认证。
- trace 透传。
- 超时。
- 重试策略。
- 错误码。
- 契约测试。
- 版本兼容。

不要因为是内部接口就随便改。

## 隔离 DTO

外部 DTO 和内部 DTO 应分开。

原因：

- 外部字段需要稳定。
- 内部模型会演进。
- 外部不应看到内部字段。
- 安全和脱敏要求不同。

直接暴露内部对象会造成长期兼容负担。

## 网关和开放平台

外部 API 通常经过：

- API 网关。
- 开放平台。
- 签名鉴权。
- 配额限流。
- 审计日志。

内部服务可以走服务网格、注册发现或内部网关。

## 在 eMall 项目中怎么讲？

商家开放 API 查询订单，只返回商家可见字段。

内部订单服务 API 可能包含履约状态、风控标记和内部审计信息。

这些不能直接暴露给商家。

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
外部 API 面向不可控调用方，必须稳定、安全、限流、签名、防重放、审计和文档化；内部 API 面向服务间调用，
可以更贴近业务协作，但仍需要认证、超时、错误码、trace 和契约测试。外部 DTO 必须和内部模型隔离。

我不会把内部领域对象直接暴露成开放 API，因为那会泄漏内部实现并锁死后续演进。
```

## 回答评分点

高分答案应该覆盖：

- 外部 API 更稳定和安全。
- 内部 API 也要治理。
- 外部 DTO 和内部 DTO 隔离。
- 不暴露内部模型。
- 网关和开放平台职责。

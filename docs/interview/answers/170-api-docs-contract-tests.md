# 170 如何写 API 文档和契约测试？

[返回按分类学习面试题](../README.md)

## 题目

如何写 API 文档和契约测试？

## 先给面试官的短答案

API 文档要描述资源、方法、路径、请求参数、响应结构、错误码、鉴权、幂等、限流、示例和版本。
契约测试用于验证服务实现是否符合文档契约，防止字段删除、类型变化、错误码变化等破坏兼容。

文档和测试要一起维护，否则文档很快过期。

## API 文档内容

应包含：

- 接口用途。
- HTTP 方法和路径。
- Header。
- Query 参数。
- Request body。
- Response body。
- 错误码。
- HTTP 状态码。
- 鉴权方式。
- 幂等要求。
- 限流规则。
- 示例。
- 版本和废弃说明。

调用方应该只靠文档就能正确接入。

## OpenAPI

可以使用 OpenAPI 规范描述接口。

优点：

- 标准化。
- 可生成文档。
- 可生成客户端。
- 可做契约校验。
- 便于网关治理。

但生成文档不代表契约正确，仍需要评审和测试。

## 契约测试测什么？

测试：

- 必填字段存在。
- 字段类型稳定。
- 枚举值兼容。
- 错误码稳定。
- 响应结构不破坏。
- 请求校验规则符合约定。

它不是替代业务测试，而是保护接口契约。

## 消费者驱动契约

消费者驱动契约关注调用方真实依赖。

如果消费者只依赖某些字段，契约测试应保护这些字段不被破坏。

适合微服务多团队协作。

## 在 eMall 项目中怎么讲？

开放平台的商家订单查询 API 必须有完整 OpenAPI 文档、签名说明、错误码说明和示例。

契约测试要保证 `orderId`、`status`、`amountCents` 等字段类型和语义不被误改。

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
API 文档要覆盖方法、路径、参数、响应、错误码、鉴权、幂等、限流、版本和示例。契约测试用于验证实现是否仍满足文档和消费者依赖，
防止删除字段、改类型、改错误码语义等破坏兼容。

我会用 OpenAPI 管理接口契约，并在 CI 中运行契约测试，让文档、代码和客户端保持一致。
```

## 回答评分点

高分答案应该覆盖：

- 文档内容完整。
- OpenAPI 标准化。
- 契约测试保护兼容。
- 消费者驱动契约。
- CI 中执行。

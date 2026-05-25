# 153 幂等键应该放 Header 还是 Body？

[返回按分类学习面试题](../README.md)

## 题目

幂等键应该放 Header 还是 Body？

## 先给面试官的短答案

两种都可以，关键是团队规范一致、网关和服务都能识别。通用技术幂等键适合放 Header，例如 `Idempotency-Key`；
强业务语义的唯一标识适合放 Body，例如 `clientRequestId`、`paymentNo`。生产中可以两者结合，
Header 作为通用幂等机制，Body 保存业务流水号。

## 放 Header 的优点

优点：

- 与业务字段解耦。
- 网关可统一识别。
- 适合通用幂等中间件。
- 不污染业务请求模型。

示例：

```text
Idempotency-Key: 01H...
```

适合开放 API 或统一网关治理。

## 放 Header 的缺点

缺点：

- 业务日志中可能不直观。
- 某些客户端容易漏传。
- 和业务唯一约束仍需映射。

Header 幂等键不能替代业务唯一键。

## 放 Body 的优点

优点：

- 业务含义清晰。
- 容易落库建唯一键。
- 便于审计和排查。
- 和业务流程强绑定。

示例：

```json
{
  "clientRequestId": "checkout-123"
}
```

适合订单创建、支付请求等业务接口。

## 放 Body 的缺点

缺点：

- 每个接口都要建模。
- 网关难以统一处理。
- 不同团队字段名可能不一致。

所以需要统一规范。

## 推荐策略

推荐：

- 对外 API 使用 `Idempotency-Key` header。
- 业务写接口也保存业务流水号。
- 服务端落库建立唯一约束。
- 重复请求返回首次处理结果。

幂等键要有有效期和命名空间，避免不同接口冲突。

## 在 eMall 项目中怎么讲？

创建订单可以要求 Header 传 `Idempotency-Key`，Body 中也有 `clientRequestId`。

订单表或幂等表对 `clientRequestId` 建唯一键。

网关可以做通用重复请求保护，订单服务做最终业务幂等兜底。

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
幂等键放 Header 还是 Body 取决于语义。通用技术幂等键适合放 Header，便于网关和中间件统一治理；
业务流水号适合放 Body，便于落库、审计和业务约束。关键是规范一致，并在服务端持久化唯一约束。

我倾向于 Header 通用幂等加 Body 业务流水号组合，网关做第一层保护，业务服务做最终幂等兜底。
```

## 回答评分点

高分答案应该覆盖：

- Header 和 Body 都可以。
- Header 适合通用治理。
- Body 适合业务唯一约束。
- 最终要落库唯一。
- 要有命名空间和有效期。

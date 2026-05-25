# 166 网关和业务服务分别做什么校验？

[返回按分类学习面试题](../README.md)

## 题目

网关和业务服务分别做什么校验？

## 先给面试官的短答案

网关做通用、粗粒度、与业务无关或弱业务相关的校验，例如认证、签名、时间戳、nonce、限流、黑白名单和基本请求大小。
业务服务做细粒度业务校验，例如库存、订单状态、优惠资格、权限范围和幂等。业务服务不能完全信任网关。

网关挡住非法流量，业务服务保证业务正确。

## 网关校验

适合网关：

- token 是否有效。
- 签名是否正确。
- timestamp/nonce。
- IP 黑白名单。
- appId 是否存在。
- 粗粒度权限。
- 限流。
- 请求体大小。
- 协议合法性。

这些可以在入口统一处理。

## 业务服务校验

适合业务服务：

- 用户是否能下单。
- 商品是否上架。
- 库存是否足够。
- 优惠券是否可用。
- 订单状态是否允许取消。
- 支付金额是否匹配。
- 幂等 key 是否重复。

这些依赖业务数据和状态。

## 为什么业务服务不能完全信任网关？

原因：

- 内部调用可能绕过网关。
- 网关配置可能错误。
- 多网关环境策略不一致。
- 安全要纵深防御。

核心业务服务仍要做必要校验。

## 避免重复过度

不是所有校验都重复做。

通用认证可在网关做，服务拿可信身份上下文。

但业务权限和资源归属必须在服务内确认。

## 在 eMall 项目中怎么讲？

网关校验用户 token 和限流。

订单服务校验用户是否能购买该商品、库存是否足够、价格是否有效、幂等 key 是否重复。

即使请求来自内部服务，订单服务仍要校验订单状态流转。

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
网关负责通用入口校验，如认证、签名、nonce、防重放、限流、黑白名单和请求大小；业务服务负责细粒度业务校验，
如资源归属、订单状态、库存、优惠资格、金额一致性和幂等。业务服务不能完全信任网关，因为内部调用、
配置错误和绕过入口都可能发生。

这是纵深防御：网关降低非法流量，业务服务保证业务状态正确。
```

## 回答评分点

高分答案应该覆盖：

- 网关做通用粗粒度校验。
- 服务做业务状态校验。
- 服务不能完全信任网关。
- 避免重复但要纵深防御。
- 能结合订单场景。

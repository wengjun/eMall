# 177 如何设计接口超时预算？

[返回按分类学习面试题](../README.md)

## 题目

如何设计接口超时预算？

## 先给面试官的短答案

超时预算是把一次请求的总耗时上限拆给网关、业务服务、缓存、数据库和下游服务。
下游超时时间必须小于上游剩余时间，否则请求即使成功返回也可能已经对调用方无意义。

大型系统不能只设置单个读取超时，而要做端到端 deadline 传播。

## 为什么需要预算？

如果用户请求总 SLA 是 `300ms`，服务内部却调用三个下游，每个下游都设置 `500ms` 超时，
那么一次请求可能拖到数秒，线程和连接都会被占住。

超时预算能避免：

- 上游已经超时，下游还在执行。
- 重试叠加导致雪崩。
- 慢请求占满线程池。
- 服务链路尾延迟不可控。

## 预算拆分

示例：

```text
网关总预算：300ms
认证鉴权：20ms
业务服务处理：80ms
库存服务：60ms
优惠服务：50ms
数据库查询：40ms
预留网络和序列化：50ms
```

预算要根据真实压测和线上分位数调整，而不是凭感觉。

## deadline 传播

请求进入系统时生成截止时间：

```text
deadline = now + 300ms
```

每次调用下游前计算剩余时间：

```text
remaining = deadline - now
```

下游超时不能超过 `remaining`，并且要预留处理和返回时间。

## 在 eMall 项目中怎么讲？

下单接口如果目标是 `500ms` 内返回，订单服务调用库存、优惠和风控时都要带上剩余预算。

如果优惠服务剩余时间只有 `30ms`，就应该快速失败或走降级，而不是再等待 `300ms`。

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
接口超时预算是端到端 SLA 的拆分。上游总预算确定后，每个内部调用都要使用剩余时间设置超时，
不能让下游超时大于上游剩余时间。否则上游已经放弃，下游还在占用资源。

我会在网关生成 deadline，在服务间透传，并结合熔断、限流和降级。预算值要基于压测和线上 P95、P99 延迟持续调整。
```

## 回答评分点

高分答案应该覆盖：

- 超时预算来自端到端 SLA。
- 下游超时要小于上游剩余时间。
- 需要 deadline 传播。
- 超时、重试、熔断要一起设计。
- 预算值要基于压测和线上分位数。

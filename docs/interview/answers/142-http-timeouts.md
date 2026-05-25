# 142 如何设置连接超时、读取超时和总超时？

[返回按分类学习面试题](../README.md)

## 题目

如何设置连接超时、读取超时和总超时？

## 先给面试官的短答案

连接超时控制建立连接的等待时间，读取超时控制等待响应数据的时间，总超时控制一次调用从开始到结束的最大耗时。
生产中三者都要设置，并且总超时要小于上游接口剩余时间预算。超时不能只看平均值，要按 P99、重试和用户体验设计。

没有超时就是把线程和连接交给下游故障支配。

## 连接超时

连接超时是建立 TCP/TLS 连接的最大等待时间。

过长会导致下游不可达时线程长时间卡住。

过短可能在网络抖动时误失败。

内网微服务通常应设置较短连接超时。

## 读取超时

读取超时是连接建立后，等待响应数据的最大时间。

下游慢处理、半开连接、网络卡顿都可能触发。

读取超时过长会占用线程和连接。

## 总超时

总超时是一次完整调用的最大耗时。

它应该覆盖：

- 获取连接。
- 建立连接。
- 发送请求。
- 等待响应。
- 读取响应。

如果只设置 connect/read timeout，没有总超时，整体耗时仍可能超出上游预算。

## 时间预算

要从入口 SLA 反推。

例如订单创建 P99 目标 500 ms，下游库存最多给 80 ms，支付预校验最多给 100 ms。

不能让单个下游超时设置成 2 秒。

否则上游请求早已超时。

## 重试影响

如果有重试，总耗时要包含多次尝试。

例如单次超时 200 ms，重试 3 次，最坏可能超过 600 ms。

重试必须配合：

- 总超时。
- 退避。
- 幂等。
- 熔断。

## 在 eMall 项目中怎么讲？

订单服务调用库存：

- connect timeout 短。
- read timeout 按库存 P99 设置。
- total timeout 小于订单接口预算。
- 超时后走熔断或返回稍后重试。

支付调用要更谨慎，超时后需要查询支付结果，不能简单重试扣款。

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
连接超时控制建连，读取超时控制等待响应，总超时控制完整调用耗时。生产中三者都要设置，
并从入口接口 SLA 反推下游时间预算。重试次数、退避和总超时要一起设计，否则重试会把一次慢调用放大成多次慢调用。

超时后还要有幂等、熔断、降级和结果确认，尤其是支付这类有副作用的调用。
```

## 回答评分点

高分答案应该覆盖：

- 区分连接、读取、总超时。
- 总超时要小于上游预算。
- 重试会放大耗时。
- 超时要配合熔断和幂等。
- 支付等副作用场景要确认结果。

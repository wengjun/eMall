# 140 WebFlux 是否一定比 MVC 性能更高？

[返回按分类学习面试题](../README.md)

## 题目

WebFlux 是否一定比 MVC 性能更高？

## 先给面试官的短答案

不一定。WebFlux 的优势是非阻塞 IO 和高并发连接场景下更高效地使用线程，但它不保证所有业务都比 MVC 快。
如果业务主要是 CPU 计算、数据库驱动阻塞、代码中大量 `.block()`，WebFlux 可能更复杂且没有性能优势。

性能取决于链路模型、瓶颈位置、团队能力和压测结果。

## MVC 的优势

Spring MVC 模型成熟。

优点：

- 一请求一线程，容易理解。
- 调试简单。
- 生态成熟。
- 事务模型清晰。
- 团队接受度高。

对普通电商交易系统，MVC 通常足够。

## WebFlux 的优势

WebFlux 使用响应式非阻塞模型。

优势场景：

- 高并发慢 IO。
- 长连接。
- SSE。
- 流式传输。
- 网关代理。
- 下游也是非阻塞客户端。

它可以用更少线程承接更多连接。

## 为什么不一定更快？

原因：

- CPU 密集任务不会因为响应式变快。
- 阻塞数据库调用会占住线程。
- `.block()` 破坏非阻塞链路。
- 响应式对象和调度有额外成本。
- 调试和排障成本更高。

如果瓶颈在数据库，换 WebFlux 不会自动解决慢 SQL。

## 压测才是依据

选择 WebFlux 要压测：

- QPS。
- P99。
- CPU。
- 线程数。
- 内存。
- GC。
- 下游延迟。
- 错误率。

不要只看框架宣传。

## 在 eMall 项目中怎么讲？

eMall 网关适合评估 WebFlux，因为它大量处理网络 IO。

订单创建不一定适合 WebFlux，因为它涉及事务、状态机、库存和支付一致性。

对核心交易链路，清晰和稳定比模型先进更重要。

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
WebFlux 不一定比 MVC 性能更高。它的优势是非阻塞 IO，适合高并发慢 IO、长连接、流式和网关类场景。
如果链路中数据库、缓存或业务代码仍是阻塞的，或者频繁 block，WebFlux 只会增加复杂度。

我会根据瓶颈和压测选择模型。网关可以 WebFlux，核心交易服务通常 MVC 加线程池、超时、熔断和限流更直接可靠。
```

## 回答评分点

高分答案应该覆盖：

- WebFlux 不一定更快。
- 优势是非阻塞 IO。
- 阻塞链路会抵消收益。
- CPU 或数据库瓶颈不能靠 WebFlux 解决。
- 选择要靠压测。

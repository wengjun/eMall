# 141 如何设置 HTTP 客户端连接池？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

如何设置 HTTP 客户端连接池？

## 先给面试官的短答案

HTTP 客户端连接池要按下游维度设置最大连接数、每路由连接数、连接获取超时、连接空闲回收、连接存活时间和指标监控。
连接池容量不能只看调用方线程数，还要看下游承载能力、实例数、P99 和超时策略。

连接池过小会排队，过大会打垮下游。

## 为什么需要连接池？

HTTP 每次新建连接会有成本：

- TCP 握手。
- TLS 握手。
- 认证。
- 内核资源。

连接池复用连接，可以降低延迟和 CPU 消耗。

但连接池本身也需要容量治理。

## 核心参数

常见参数：

- max connections。
- max connections per route。
- connection acquire timeout。
- connect timeout。
- read timeout。
- keep alive。
- idle eviction。
- max connection lifetime。

不同客户端名字不同，但思路一致。

## 每个下游单独配置

不要所有下游共享一套连接池参数。

支付、库存、推荐、物流的 SLA 和容量不同。

核心下游应有独立连接池、独立指标和独立熔断。

## 容量估算

连接数要结合：

- 调用 QPS。
- 平均耗时和 P99。
- 下游实例数。
- 下游限流阈值。
- 调用方实例数。
- 是否有重试。

例如 20 个调用方实例，每个实例给支付开 500 连接，总连接可能是 10000，下游未必承受得住。

## 监控指标

必须监控：

- active connections。
- idle connections。
- pending acquire。
- acquire latency。
- timeout count。
- error count。
- per-host connection usage。

连接池 pending 升高通常会直接推高接口 P99。

## 在 eMall 项目中怎么讲？

订单服务调用库存和支付时，应该分别设置连接池。

库存高峰 QPS 大，连接池和线程池要受库存服务容量约束。

支付下游通常更敏感，连接池、超时和重试要更保守，避免重复支付和故障放大。

## 深度增强：Spring 服务治理图

![Spring 微服务调用栈和治理边界](../../assets/spring-service-stack.svg)

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
HTTP 连接池要按下游设置最大连接数、每路由连接数、获取连接超时、连接超时、读取超时、空闲回收和连接生命周期。
容量要根据 QPS、耗时、调用方实例数、下游实例数和下游限流能力估算，不能只在调用方无限放大。

生产上我会监控 active、idle、pending acquire、acquire latency 和 timeout，并和线程池、熔断、限流一起治理。
```

## 回答评分点

高分答案应该覆盖：

- 连接池要按下游配置。
- 最大连接和每路由连接。
- 获取连接超时很重要。
- 容量要看下游承载。
- 监控 pending 和 acquire latency。

## 深度完善：面向 L6 的回答框架

围绕「如何设置 HTTP 客户端连接池？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「Spring Boot 和服务工程」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `各服务的 Controller、ApplicationService、MyBatis Plus Mapper、Actuator 和 RestClient` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：事务失败率、健康检查状态、依赖调用耗时、配置变更次数、启动耗时。
- 验证证据：Spring Boot 测试、集成测试、配置审计、Actuator 指标和链路 Trace。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：如何设置 HTTP 客户端连接池？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。


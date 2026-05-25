# 137 readiness 和 liveness 在 Spring 中如何实现？

[返回按分类学习面试题](../README.md)

## 题目

readiness 和 liveness 在 Spring 中如何实现？

## 先给面试官的短答案

Spring Boot Actuator 支持 Kubernetes probes，可以通过 health groups 暴露 liveness 和 readiness。
通常 liveness 表示应用进程是否存活，readiness 表示是否可以接流量。可以使用 Actuator 默认探针，
也可以自定义 `HealthIndicator` 或 `AvailabilityChangeEvent` 控制可用状态。

关键是不要把弱依赖放进 liveness。

## Actuator 支持

Spring Boot 可以暴露：

```text
/actuator/health/liveness
/actuator/health/readiness
```

用于 Kubernetes 探针。

需要引入 Actuator 并启用相关配置。

## AvailabilityState

Spring Boot 内部有可用性状态：

- LivenessState。
- ReadinessState。

应用启动和关闭过程中，状态会变化。

Kubernetes 可以根据这些端点判断是否重启或摘流。

## 自定义 HealthIndicator

可以实现：

```java
class DatabaseHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        return Health.up().build();
    }
}
```

但要控制检查耗时和依赖范围。

健康检查本身不能成为系统负担。

## readiness 自定义

readiness 可以包含：

- 数据库连接。
- 核心缓存。
- 必要配置。
- 消息组件。

弱依赖不要放进 readiness，除非没有它确实不能接流量。

## liveness 自定义

liveness 应轻量。

通常只判断应用是否还在正常运行。

不要检查数据库、Redis、支付下游。

否则下游故障会导致应用被平台重启，造成更大故障。

## 在 eMall 项目中怎么讲？

订单服务：

- liveness：应用进程和 Web 容器存活。
- readiness：订单库连接、核心配置、消息 outbox 可用。

推荐服务不可用时，订单服务 readiness 不应失败，而应通过降级隐藏推荐信息。

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
Spring Boot Actuator 可以暴露 /actuator/health/liveness 和 /actuator/health/readiness，
也可以通过 HealthIndicator 和 AvailabilityChangeEvent 自定义状态。liveness 用于判断是否重启，
应该轻量；readiness 用于判断是否接流量，可以检查关键依赖和初始化。

我会把强依赖放入 readiness，把弱依赖交给熔断降级处理，避免健康检查导致故障扩散。
```

## 回答评分点

高分答案应该覆盖：

- Actuator health probes。
- liveness 和 readiness 含义不同。
- HealthIndicator 可自定义。
- liveness 要轻量。
- 强弱依赖区分。

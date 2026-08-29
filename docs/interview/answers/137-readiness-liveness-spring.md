# 137 readiness 和 liveness 在 Spring 中如何实现？

[返回按分类学习面试题](../README.md)

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

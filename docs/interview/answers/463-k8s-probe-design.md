# 463 readinessProbe、livenessProbe、startupProbe 如何设计？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

readinessProbe 判断 Pod 是否可以接收流量，livenessProbe 判断容器是否需要重启，startupProbe 判断
应用是否完成启动。三者目标不同，不能用同一个重型健康检查代替。

设计原则是 readiness 偏流量准入，liveness 偏进程自愈，startup 保护慢启动。

## readinessProbe

用于：

- 控制是否加入 Service 负载均衡。
- 发布时避免未就绪实例接流量。
- 依赖初始化未完成时拒绝流量。

readiness 失败不会重启容器，只会摘流量。

## livenessProbe

用于：

- 发现进程死锁。
- 发现不可恢复卡死。
- 触发容器重启。

liveness 不应因为某个下游故障就失败，否则会导致无意义重启。

## startupProbe

用于：

- 保护启动慢的 Java 服务。
- 避免启动过程中被 liveness 杀掉。
- 给初始化任务更多时间。

startup 成功后，liveness 才开始发挥作用。

## 电商系统实践

大型电商系统的 Spring Boot 服务可以用 `/actuator/health/readiness` 判断是否能接流量，用轻量本地检查作为
liveness，用 startupProbe 给 JVM 启动和缓存初始化足够时间。

不能让 liveness 强依赖 MySQL、Redis、Kafka 全部健康。

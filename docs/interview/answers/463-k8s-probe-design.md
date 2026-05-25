# 463 readinessProbe、livenessProbe、startupProbe 如何设计？

[返回按分类学习面试题](../README.md)

## 题目

readinessProbe、livenessProbe、startupProbe 如何设计？

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

## 在 eMall 项目中怎么讲？

eMall Spring Boot 服务可以用 `/actuator/health/readiness` 判断是否能接流量，用轻量本地检查作为
liveness，用 startupProbe 给 JVM 启动和缓存初始化足够时间。

不能让 liveness 强依赖 MySQL、Redis、Kafka 全部健康。

## 深度增强：Kubernetes 运维治理图

![Kubernetes 生产运行和故障治理](../assets/kubernetes-operations.svg)

Kubernetes 题不能只背 Deployment、Service 和 Ingress。生产稳定性还取决于资源 requests/limits、探针、HPA、PDB、
灰度发布、配置回滚、日志指标 Trace 和故障 Runbook。

## 深度增强：Java 17 发布门禁示例

```java
record ReleaseSignal(double errorRate, long p99Millis, double cpuThrottleRate, boolean rollbackSafe) {

    boolean canContinue() {
        return errorRate < 0.001
                && p99Millis < 300
                && cpuThrottleRate < 0.05
                && rollbackSafe;
    }
}
```

这段代码表达发布平台的核心：放量不是人工拍脑袋，而是由错误率、延迟、资源和回滚安全共同决定。

## 深度增强：生产边界

K8s 会重启失败容器，但不保证业务一定恢复。错误的 liveness probe 可能造成重启风暴；
过低的 CPU limit 会造成 throttling；不兼容数据库变更会让回滚失效。平台能力要和应用设计配合。

## 深度增强：面试高分表达

我会把 K8s 视为运行平台，而不是稳定性的全部答案。真正生产级要有容量规划、灰度门禁、配置治理、可观测性、
自动回滚和数据库兼容检查，才能支撑核心交易链路。

## 专家级完整回答

```text
readinessProbe 控制流量准入，失败后 Pod 从 Service 端点摘除；livenessProbe 判断进程是否不可恢复，
失败会重启容器；startupProbe 用来保护慢启动应用，启动成功前延迟 liveness 检查。

生产中三者要分开设计。readiness 可以检查关键依赖状态，liveness 应尽量轻量且不强依赖下游。
```

## 回答评分点

高分答案应该覆盖：

- readiness 控制接流量。
- liveness 控制重启。
- startup 保护慢启动。
- liveness 不应强依赖下游。
- Java 服务启动慢要用 startupProbe。

# 464 为什么 liveness 不应该强依赖所有下游？

[返回按分类学习面试题](../README.md)

## 题目

为什么 liveness 不应该强依赖所有下游？

## 先给面试官的短答案

liveness 失败会触发容器重启。如果它强依赖所有下游，那么 Redis、数据库或第三方服务短暂故障时，
大量业务 Pod 会被 Kubernetes 同时重启，导致故障扩大。

liveness 应判断本进程是否活着，而不是判断全世界是否健康。

## 问题场景

如果 liveness 检查 Redis：

- Redis 抖动。
- 所有依赖 Redis 的 Pod liveness 失败。
- Kubernetes 重启大量 Pod。
- 启动风暴增加系统压力。
- 原故障被放大。

这就是级联故障。

## 正确职责

liveness 适合检查：

- 进程是否卡死。
- 主线程是否无响应。
- 本地事件循环是否可用。
- 不可恢复死锁。

它不适合检查所有下游依赖。

## 下游健康放哪里

下游健康更适合：

- readiness。
- 依赖监控。
- 熔断器状态。
- 业务降级策略。
- 告警系统。

下游故障应摘流量或降级，不一定重启本服务。

## 在 eMall 项目中怎么讲？

eMall 订单服务如果 Redis 不可用，liveness 不应失败重启订单 Pod。

订单服务应通过 readiness 或业务降级控制接流量，同时启用本地限流和数据库保护。重启订单服务不能
修复 Redis。

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
liveness 的语义是判断当前容器是否需要重启。如果把 Redis、MySQL、Kafka 和第三方全部纳入 liveness，
下游抖动会触发大量 Pod 重启，造成级联故障。

正确做法是 liveness 只检查本进程是否不可恢复；下游依赖通过 readiness、熔断、降级、监控和告警
处理。
```

## 回答评分点

高分答案应该覆盖：

- liveness 失败会重启容器。
- 下游故障不一定靠重启解决。
- 强依赖下游会级联重启。
- liveness 检查本进程健康。
- 下游健康交给 readiness 和降级。

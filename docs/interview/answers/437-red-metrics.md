# 437 RED 指标是什么？

[返回按分类学习面试题](../README.md)

## 题目

RED 指标是什么？

## 先给面试官的短答案

RED 是 Rate、Errors、Duration，分别表示请求速率、错误数或错误率、请求耗时。它常用于服务级
监控，能快速判断一个在线服务是否健康。

对电商接口来说，RED 是接口监控的基础。

## Rate

Rate 表示请求速率。

关注：

- QPS。
- 每分钟请求量。
- 不同接口流量。
- 不同来源流量。

流量突增或突降都可能是异常。

## Errors

Errors 表示错误。

关注：

- 5xx 错误率。
- 业务失败率。
- 超时率。
- 下游错误率。
- 限流拒绝率。

错误要区分系统错误和业务拒绝。

## Duration

Duration 表示耗时。

关注：

- 平均耗时。
- P95。
- P99。
- 最大耗时。

生产排障更关注分位数，而不是只看平均值。

## 在 eMall 项目中怎么讲？

eMall 下单接口要监控 QPS、下单失败率和 P99 延迟。

如果 QPS 正常但错误率升高，可能是库存、价格或风控异常。如果 P99 升高，可能是下游慢或线程池
排队。

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
RED 指标是 Rate、Errors、Duration，用于监控请求型服务。Rate 看流量，Errors 看失败，Duration
看延迟。

在微服务系统中，每个核心接口都应该有 RED 指标，并按接口、状态码、错误码和依赖维度拆分。它能
帮助快速发现服务是否异常以及异常影响范围。
```

## 回答评分点

高分答案应该覆盖：

- Rate 是请求速率。
- Errors 是错误率。
- Duration 是耗时。
- 适合服务接口监控。
- 要看 P95/P99 而不只是平均值。

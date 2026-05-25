# 438 USE 指标是什么？

[返回按分类学习面试题](../README.md)

## 题目

USE 指标是什么？

## 先给面试官的短答案

USE 是 Utilization、Saturation、Errors，分别表示资源利用率、资源饱和度和错误数。它常用于分析
底层资源，例如 CPU、内存、磁盘、网络、线程池和连接池。

RED 看服务请求，USE 看资源瓶颈。

## Utilization

Utilization 表示资源使用比例。

例如：

- CPU 使用率。
- 内存使用率。
- 磁盘使用率。
- 网络带宽使用率。
- 连接池使用率。

使用率高不一定已经故障，但需要结合饱和度看。

## Saturation

Saturation 表示资源是否排队或过载。

例如：

- CPU run queue。
- 线程池队列长度。
- 连接池等待数。
- 磁盘 IO 队列。
- 网络丢包或排队。

饱和度高通常直接影响延迟。

## Errors

Errors 表示资源层错误。

例如：

- 磁盘错误。
- 网络错误。
- 连接获取失败。
- 线程池拒绝。
- GC 失败或 OOM。

资源错误往往是故障信号。

## 在 eMall 项目中怎么讲？

eMall 订单服务 P99 升高时，除了看接口 RED，还要看数据库连接池 USE。

如果连接池利用率 100%、等待队列增长、获取连接超时增加，就说明瓶颈可能在数据库连接池或数据库。

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
USE 指标是 Utilization、Saturation、Errors，适合排查资源瓶颈。Utilization 看资源使用率，
Saturation 看是否排队或过载，Errors 看资源层错误。

RED 更适合服务请求层，USE 更适合 CPU、内存、磁盘、网络、线程池和连接池。生产排障常常需要
先用 RED 发现接口异常，再用 USE 定位资源瓶颈。
```

## 回答评分点

高分答案应该覆盖：

- Utilization 是利用率。
- Saturation 是饱和和排队。
- Errors 是资源错误。
- USE 适合资源排查。
- RED 和 USE 互补。

# 466 requests 和 limits 如何设置？

[返回按分类学习面试题](../README.md)

## 题目

requests 和 limits 如何设置？

## 先给面试官的短答案

requests 表示调度时保留的资源，limits 表示容器可使用的资源上限。设置要基于压测和生产观测，
保证 Pod 能稳定运行，同时避免资源浪费和节点过载。

Java 服务尤其要让 JVM 内存配置和容器 memory limit 匹配。

## requests

requests 用于：

- Kubernetes 调度。
- 资源预留。
- HPA 计算 CPU 利用率基准。
- 确保节点容量规划。

requests 太低会导致节点过度打包。

## limits

limits 用于：

- 限制最大资源使用。
- 防止单个容器拖垮节点。
- memory 超限会 OOMKill。
- CPU limit 会触发 throttling。

limits 太紧会导致性能抖动。

## 设置方法

方法：

- 先压测估算基线。
- 观察生产 P95/P99 资源使用。
- request 设置为稳定运行需要的资源。
- limit 设置为可接受峰值。
- 定期根据真实负载调整。

不要拍脑袋设置。

## 在 eMall 项目中怎么讲？

eMall 订单服务如果稳定需要 1 核和 1.5GB 内存，可以把 request 设在接近这个水平，limit 给合理峰值。

同时 JVM `-Xmx` 要小于容器 memory limit，给 metaspace、线程栈和 direct memory 留空间。

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
requests 是调度和资源预留依据，limits 是容器使用上限。设置要基于压测和生产观测，而不是固定模板。

Java 服务要特别注意内存 limit 和 JVM 参数匹配。CPU limit 可能导致 throttling，memory limit
过低会 OOMKill。资源配置要定期复盘和调整。
```

## 回答评分点

高分答案应该覆盖：

- requests 用于调度和预留。
- limits 是资源上限。
- CPU limit 可能 throttling。
- memory limit 可能 OOMKill。
- Java JVM 内存要匹配容器限制。

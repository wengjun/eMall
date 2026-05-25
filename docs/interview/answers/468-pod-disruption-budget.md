# 468 PodDisruptionBudget 解决什么问题？

[返回按分类学习面试题](../README.md)

## 题目

PodDisruptionBudget 解决什么问题？

## 先给面试官的短答案

PodDisruptionBudget 用于限制自愿中断期间可同时不可用的 Pod 数量，例如节点维护、驱逐和集群升级。
它能防止维护操作一次性驱逐太多副本，导致服务容量不足。

PDB 保护的是可用性，不处理应用自身崩溃这种非自愿中断。

## 自愿中断

自愿中断包括：

- 节点维护 drain。
- 集群升级。
- 人工驱逐 Pod。
- 节点缩容。

这些场景 Kubernetes 可以遵守 PDB。

## 配置方式

常见配置：

- `minAvailable`。
- `maxUnavailable`。

例如订单服务 5 个副本，可以要求至少 4 个可用，维护时一次最多影响 1 个。

## 注意点

注意：

- PDB 不能替代多副本部署。
- PDB 不处理节点突然宕机。
- PDB 可能阻塞节点维护。
- 需要结合 readinessProbe。
- 单副本服务设置 PDB 意义有限。

PDB 要和容量规划一起看。

## 在 eMall 项目中怎么讲？

eMall 订单服务和支付服务应配置 PDB，避免集群升级时同时驱逐多个 Pod。

如果服务只有两个副本且 PDB 要求 `minAvailable: 2`，节点维护可能无法进行，因此副本数和 PDB 要
一起设计。

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
PDB 用于控制自愿中断期间的最小可用副本数或最大不可用副本数。它防止节点维护、drain 或集群升级
时一次性驱逐太多 Pod，影响服务可用性。

PDB 不处理应用崩溃和节点突然宕机，也不能替代多副本和容量规划。它要结合 readinessProbe、滚动
发布和副本数一起设计。
```

## 回答评分点

高分答案应该覆盖：

- PDB 控制自愿中断。
- minAvailable 和 maxUnavailable。
- 防止维护时可用副本过少。
- 不处理非自愿故障。
- 要结合副本数和 readiness。

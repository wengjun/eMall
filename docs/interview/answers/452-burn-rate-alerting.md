# 452 burn rate 告警如何理解？

[返回按分类学习面试题](../README.md)

## 题目

burn rate 告警如何理解？

## 先给面试官的短答案

burn rate 表示错误预算消耗速度。burn rate 告警不是只看当前错误率，而是看错误预算是否被过快
消耗。它能同时捕捉短时间严重故障和长时间缓慢劣化。

高 burn rate 意味着如果持续下去，SLO 会很快被打穿。

## 基本概念

如果一个月错误预算允许 0.1% 失败，但当前失败率远高于这个水平，就说明预算正在加速燃烧。

burn rate 越高，留给团队响应的时间越短。

## 多窗口告警

常见做法：

- 短窗口发现快速事故。
- 长窗口发现持续劣化。
- 两个窗口同时满足再告警。

这样可以降低偶发尖刺带来的误报。

## 优点

优点：

- 和 SLO 直接关联。
- 按业务影响告警。
- 降低噪声。
- 能指导响应优先级。
- 适合可靠性管理。

比单纯 CPU 或错误率阈值更贴近用户体验。

## 在 eMall 项目中怎么讲？

eMall 下单 SLO 如果错误预算在 1 小时内消耗过快，说明下单链路正在发生严重事故，应触发 P0 或 P1
告警。

如果错误率只是短暂尖刺但长期窗口正常，可以降低误报。

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
burn rate 是错误预算消耗速度。它把错误率和 SLO 目标结合起来判断事故严重程度。如果 burn rate
很高，说明按照当前失败速度，错误预算会很快耗尽。

生产中常用多窗口 burn rate 告警，同时覆盖短时严重故障和长时间缓慢劣化。它比固定阈值更贴近
用户影响和可靠性目标。
```

## 回答评分点

高分答案应该覆盖：

- burn rate 是预算消耗速度。
- 与 SLO 直接关联。
- 高 burn rate 表示会打穿 SLO。
- 多窗口降低误报。
- 比固定阈值更贴近业务影响。

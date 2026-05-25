# 453 Runbook 应该包含什么？

[返回按分类学习面试题](../README.md)

## 题目

Runbook 应该包含什么？

## 先给面试官的短答案

Runbook 是故障处理手册，应包含告警含义、影响判断、快速检查项、常见原因、应急操作、回滚步骤、
升级联系人、验证方式和事后记录要求。

好的 Runbook 能让值班人员在压力下按步骤处理，而不是临场猜。

## 基本信息

应包含：

- 告警名称。
- 影响服务。
- 业务影响。
- 告警等级。
- 负责人。
- 相关 Dashboard。
- 相关日志查询。

第一步是让人知道发生了什么。

## 排查步骤

步骤：

- 如何确认影响范围。
- 看哪些指标。
- 查哪些日志。
- 看哪些 Trace。
- 如何区分常见原因。
- 如何判断是否升级。

步骤要具体到命令或链接。

## 应急和恢复

包含：

- 限流方式。
- 降级开关。
- 回滚步骤。
- 扩容步骤。
- 数据修复步骤。
- 验证恢复的指标。

应急操作必须可执行。

## 在 eMall 项目中怎么讲？

eMall “下单成功率下降” Runbook 应包含下单漏斗 Dashboard、库存和价格服务指标、订单错误码查询、
降级策略、回滚入口和值班升级联系人。

恢复后要确认下单成功率、支付成功率和订单状态积压恢复正常。

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
Runbook 要把告警处理流程标准化。它应包含告警含义、业务影响、Dashboard、日志和 Trace 查询、
排查步骤、常见原因、应急动作、回滚步骤、升级联系人和恢复验证方式。

好的 Runbook 应该让非系统作者也能按步骤完成初步处理，并降低事故中的沟通和决策成本。
```

## 回答评分点

高分答案应该覆盖：

- 告警含义和影响。
- 排查步骤和链接。
- 应急操作和回滚。
- 升级联系人。
- 恢复验证方式。

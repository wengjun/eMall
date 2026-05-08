# 469 滚动升级如何保证可用性？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

滚动升级如何保证可用性？

## 先给面试官的短答案

滚动升级通过分批替换旧 Pod，配合多副本、readinessProbe、maxUnavailable、maxSurge、PDB、优雅
关闭和自动回滚来保证服务持续可用。

关键是新实例真正就绪后再接流量，旧实例处理完请求后再退出。

## 关键配置

配置：

- 副本数大于 1。
- readinessProbe。
- `maxUnavailable`。
- `maxSurge`。
- terminationGracePeriod。
- preStop hook。
- PDB。

这些共同控制发布过程。

## 风险点

风险：

- readiness 过早成功。
- 旧 Pod 被立即杀死。
- 连接未排空。
- 新版本启动慢。
- 数据库变更不兼容。
- 下游依赖未准备好。

滚动升级不自动解决兼容性问题。

## 验证方式

验证：

- 观察错误率。
- 观察 P99。
- 观察 readiness 状态。
- 小流量灰度。
- 自动回滚条件。
- 业务指标确认。

发布成功不等于 Pod 全部 Running。

## 在 eMall 项目中怎么讲？

eMall 订单服务升级时，设置 `maxUnavailable: 0` 和合理 `maxSurge`，让新 Pod 通过 readiness 后再接
流量。

旧 Pod 收到终止信号后先从流量中摘除，等待正在执行的下单请求完成，再退出。

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
滚动升级保证可用性的核心是逐步替换、先就绪后接流量、先摘流量后退出。需要多副本、readiness、
maxUnavailable、maxSurge、PDB、preStop 和优雅关闭配合。

还要保证新旧版本兼容，尤其是数据库 schema、消息 schema 和缓存 key。否则滚动发布过程会出现
新旧版本互相不兼容。
```

## 回答评分点

高分答案应该覆盖：

- 多副本和分批替换。
- readiness 决定接流量。
- maxUnavailable 和 maxSurge。
- 优雅关闭和连接排空。
- 新旧版本兼容。

## 深度完善：面向 L6 的回答框架

围绕「滚动升级如何保证可用性？」，高分答案不能停在概念定义，而要把「镜像、Probe、HPA、资源限制、滚动升级、灰度、回滚、多 AZ 和 Service Mesh」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「容器、Kubernetes 和发布」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `ops/k8s、release、platform-ops、gateway 和核心业务服务的部署清单` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：Pod 重启数、Probe 失败数、CPU throttle、HPA 扩缩容次数、灰度错误率。
- 验证证据：Deployment、PDB、HPA、灰度记录、回滚记录、资源曲线和演练报告。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：滚动升级如何保证可用性？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

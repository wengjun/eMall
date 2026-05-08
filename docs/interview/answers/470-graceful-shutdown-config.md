# 470 优雅关闭如何配置？

[返回按分类学习面试题](../README.md)

## 题目

优雅关闭如何配置？

## 先给面试官的短答案

优雅关闭要让服务先停止接收新流量，再等待正在处理的请求完成，最后释放资源并退出。Kubernetes 中
通常结合 readiness 摘流量、preStop、terminationGracePeriod 和应用自身 shutdown hook。

核心目标是避免正在执行的订单、支付和消息消费被强制中断。

## Kubernetes 配置

配置：

- readinessProbe 失败后摘流量。
- preStop hook 延迟或调用下线接口。
- terminationGracePeriodSeconds 足够长。
- 避免立即 `SIGKILL`。
- PDB 控制同时中断数量。

Kubernetes 只提供机制，应用也要配合。

## 应用侧处理

应用要做：

- 停止接收新请求。
- 等待 in-flight 请求完成。
- 停止拉取新消息。
- 提交或回滚当前事务。
- 关闭线程池。
- 关闭连接池。

消息消费者要特别注意 offset 提交。

## 时间设置

时间要参考：

- 接口 P99。
- 最长事务时间。
- 消息处理最长时间。
- 下游超时时间。
- 发布速度要求。

时间太短会中断请求，太长会拖慢发布。

## 在 eMall 项目中怎么讲？

eMall 订单服务关闭时，先通过 readiness 从 Service 端点摘除，再等待正在执行的下单请求完成。

Kafka 消费者停止拉取新消息，处理完当前消息并提交 offset 后再退出，避免重复或半处理状态。

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
优雅关闭要分两步：先摘流量，后等待在途请求完成。Kubernetes 中通过 readiness、preStop 和
terminationGracePeriod 配合，应用中通过 shutdown hook 停止接收请求、等待线程池、提交事务和关闭
连接。

对电商核心链路来说，优雅关闭能避免下单、支付回调和消息消费在发布过程中被强制中断。
```

## 回答评分点

高分答案应该覆盖：

- 先停止接新流量。
- 等待在途请求完成。
- Kubernetes 配置 readiness、preStop 和 grace period。
- 应用 shutdown hook 配合。
- 消息消费者要处理 offset。

## 深度完善：面向 L6 的回答框架

围绕「优雅关闭如何配置？」，高分答案不能停在概念定义，而要把「镜像、Probe、HPA、资源限制、滚动升级、灰度、回滚、多 AZ 和 Service Mesh」讲成一条可验证的工程链路。
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

本题复习重点：优雅关闭如何配置？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

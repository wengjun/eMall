# 467 CPU limit 对 Java 服务有什么影响？

[返回按分类学习面试题](../README.md)

## 题目

CPU limit 对 Java 服务有什么影响？

## 先给面试官的短答案

CPU limit 会让容器超过配额后被 throttling。对 Java 服务来说，这会导致请求延迟升高、GC 变慢、
线程调度变慢、P99 抖动和吞吐下降。

CPU 使用率看起来不一定满，但 throttling 已经在影响延迟。

## throttling

CPU limit 通过 CFS 配额控制。

当容器在一个周期内用完 CPU 配额后，会被暂停到下个周期。暂停期间应用线程无法继续执行。

## 对 Java 的影响

影响：

- 业务线程执行变慢。
- GC 线程执行变慢。
- ForkJoinPool 并发度受影响。
- 定时任务延迟。
- P99 明显升高。
- 超时和重试增加。

尾延迟经常先暴露问题。

## 排查指标

看：

- CPU throttled time。
- CPU throttled periods。
- P99 延迟。
- GC pause。
- 线程池队列。
- 容器 CPU usage。

不能只看 CPU 平均使用率。

## 在 eMall 项目中怎么讲？

eMall 订单服务在高峰期 P99 抖动，如果 CPU throttling 明显，即使平均 CPU 只有 60%，也可能是
CPU limit 太紧造成。

可以提高 limit、优化线程池、降低同步计算或调整 HPA 指标。

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
CPU limit 会导致容器超过 CFS 配额后被 throttling。Java 服务受影响明显，因为业务线程、GC 线程、
线程池和定时任务都会被暂停或延迟执行。

排查时要看 throttled periods、throttled time、P99、GC 和线程池队列。CPU 平均值正常不代表没有
CPU limit 问题。
```

## 回答评分点

高分答案应该覆盖：

- CPU limit 会导致 throttling。
- Java 线程和 GC 都会受影响。
- P99 和尾延迟升高。
- 需要看 throttling 指标。
- 平均 CPU 不足以判断。

## 深度完善：面向 L6 的回答框架

围绕「CPU limit 对 Java 服务有什么影响？」，高分答案不能停在概念定义，而要把「镜像、Probe、HPA、资源限制、滚动升级、灰度、回滚、多 AZ 和 Service Mesh」讲成一条可验证的工程链路。
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
本题复习重点：CPU limit 对 Java 服务有什么影响？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

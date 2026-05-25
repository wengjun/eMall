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

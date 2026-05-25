# 465 HPA 根据什么指标扩缩容？

[返回按分类学习面试题](../README.md)

## 题目

HPA 根据什么指标扩缩容？

## 先给面试官的短答案

HPA 可以根据 CPU、内存、自定义指标和外部指标扩缩容。常见自定义指标包括 QPS、请求延迟、队列
长度、Kafka lag、线程池利用率等。

扩缩容指标要能反映负载，并且扩容后确实能缓解瓶颈。

## 资源指标

资源指标：

- CPU 使用率。
- 内存使用率。

CPU 是最常见指标，但不是所有服务都适合只按 CPU 扩容。

## 自定义指标

自定义指标：

- QPS。
- P95 或 P99 延迟。
- 线程池队列长度。
- 请求并发数。
- 连接池等待数。
- Kafka lag。

这些更贴近业务负载。

## 外部指标

外部指标：

- 消息队列积压。
- 云负载均衡指标。
- 第三方监控指标。
- 业务活动流量预测。

适合异步消费者和任务处理系统。

## 在 eMall 项目中怎么讲？

eMall gateway 可以按 CPU 和 QPS 扩容。订单服务可以结合 CPU、请求延迟和线程池队列。

Kafka 消费者更适合按 lag 或每个 Pod 待处理消息数扩容，而不是只看 CPU。

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
HPA 支持 CPU、内存、自定义指标和外部指标。指标选择要和瓶颈匹配。在线接口可以看 CPU、QPS、
延迟和并发，异步消费者可以看 Kafka lag 或队列长度。

扩容不是万能的。如果瓶颈在数据库、单分区热点或下游限流，增加 Pod 可能无法解决问题，甚至会
放大压力。
```

## 回答评分点

高分答案应该覆盖：

- HPA 可基于 CPU 和内存。
- 支持自定义和外部指标。
- 在线服务和消费者指标不同。
- 指标要匹配瓶颈。
- 扩容可能放大下游压力。

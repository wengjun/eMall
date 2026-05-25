# 444 Kafka lag 增长如何排查？

[返回按分类学习面试题](../README.md)

## 题目

Kafka lag 增长如何排查？

## 先给面试官的短答案

Kafka lag 增长要先判断是生产速率突增还是消费速率下降，再按 Topic、Consumer Group、Partition
维度看 lag 分布。然后检查消费者耗时、错误率、下游依赖、Rebalance、单分区热点和 Broker 状态。

总 lag 增长只是现象，原因通常在消费端或下游。

## 先看趋势

先看：

- Producer 写入 TPS。
- Consumer 消费 TPS。
- lag 增长速度。
- 最大 Partition lag。
- lag 持续时间。

如果写入突增，可能是流量问题；如果消费下降，重点看消费者。

## 消费端排查

检查：

- 消费者实例是否减少。
- 单条处理耗时是否升高。
- 下游接口是否变慢。
- 数据库是否慢。
- 是否有毒消息反复失败。
- offset 提交是否异常。

消费端慢是最常见原因。

## Kafka 侧排查

检查：

- Broker CPU。
- 磁盘 IO。
- 网络带宽。
- under replicated partitions。
- Rebalance 次数。
- Partition 倾斜。

Broker 异常也会影响消费。

## 在 eMall 项目中怎么讲？

eMall `order-events` 的搜索消费组 lag 增长时，先看是否只有搜索组积压。如果库存组正常，说明订单
事件生产正常，问题可能在搜索消费者或 OpenSearch。

如果只有某个 Partition lag 高，重点排查分区键热点。

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
Kafka lag 增长要先比较生产速率和消费速率，再按 Topic、Group、Partition 拆分。总 lag 只能说明
积压，最大 Partition lag 能暴露热点。

排查消费者处理耗时、错误重试、下游延迟、Rebalance、offset 提交和 Broker 资源。处理方式要根据
瓶颈决定，不能盲目加消费者。
```

## 回答评分点

高分答案应该覆盖：

- 比较生产和消费速率。
- 按 Topic、Group、Partition 拆分。
- 最大 Partition lag 很关键。
- 检查消费者和下游。
- 不能盲目加消费者。

# 443 Outbox 积压如何排查？

[返回按分类学习面试题](../README.md)

## 题目

Outbox 积压如何排查？

## 先给面试官的短答案

Outbox 积压要看事件生产速率、Relay 扫描速率、投递 MQ 成功率、数据库扫描效率、锁竞争、失败重试
和 MQ 可用性。核心指标是未发送数量、最老未发送时间和发送失败原因。

Outbox 积压会导致读模型、搜索、履约和通知延迟。

## 排查方向

方向：

- 是否业务流量突增。
- Relay 实例是否存活。
- 扫描 SQL 是否变慢。
- Outbox 表是否膨胀。
- MQ 是否不可用。
- 发送失败是否重试风暴。
- 多实例是否锁竞争。

先判断是生产太快还是消费太慢。

## 关键指标

指标：

- `NEW` 事件数量。
- `FAILED` 事件数量。
- 最老 `NEW` 事件时间。
- Relay 每秒发送数。
- MQ 发送成功率。
- 扫描耗时。
- 单批处理耗时。

最老未发送时间比总量更能体现业务影响。

## 处理方式

方式：

- 扩容 Relay。
- 优化扫描索引。
- 分片扫描。
- 限速业务写入。
- 修复 MQ 连接。
- 失败事件隔离。
- 清理历史已发送数据。

扩容前要确认不是 MQ 或数据库瓶颈。

## 在 eMall 项目中怎么讲？

eMall 订单 Outbox 积压会导致库存、履约和搜索延迟。先看 `order-created` 事件最老未发送时间。

如果扫描 SQL 慢，应检查 `status, created_at` 索引和历史表膨胀。如果 MQ 发送失败，要优先恢复 MQ
并限速补发。

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
Outbox 积压要比较事件写入速率和 Relay 投递速率。排查 Relay 存活、扫描 SQL、表膨胀、MQ 发送、
失败重试和多实例抢占。

关键指标是 NEW 数量、FAILED 数量、最老未发送时间、扫描耗时和发送成功率。处理上可以扩容 Relay、
优化索引、分片扫描、隔离失败事件和限速补发。
```

## 回答评分点

高分答案应该覆盖：

- 比较生产和投递速率。
- 关注最老未发送时间。
- 检查 Relay、SQL、MQ 和失败重试。
- 表膨胀会影响扫描。
- 恢复后要限速补发。

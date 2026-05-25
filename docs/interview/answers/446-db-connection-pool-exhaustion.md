# 446 数据库连接池耗尽如何排查？

[返回按分类学习面试题](../README.md)

## 题目

数据库连接池耗尽如何排查？

## 先给面试官的短答案

数据库连接池耗尽要检查连接是否被慢 SQL、长事务、连接泄露、突增流量、线程池堆积或数据库变慢占住。
关键指标是活跃连接数、等待连接数、获取连接耗时、慢 SQL、事务耗时和数据库端连接数。

不要第一反应只调大连接池，可能会把数据库压垮。

## 应看指标

指标：

- active connections。
- idle connections。
- pending threads。
- connection acquire time。
- connection timeout count。
- SQL P99。
- transaction duration。

连接池耗尽通常伴随等待队列增长。

## 常见原因

原因：

- 慢 SQL 增加。
- 长事务占用连接。
- 外部调用放在事务内。
- 连接未关闭。
- 请求流量突增。
- 数据库 CPU 或 IO 高。
- 连接池配置过小或过大。

事务边界不合理很常见。

## 处理方式

处理：

- 找出慢 SQL。
- 缩短事务。
- 禁止事务内远程调用。
- 修复连接泄露。
- 限流保护。
- 优化索引。
- 合理设置连接池大小。

先降低连接占用时间，再谈扩容。

## 在 eMall 项目中怎么讲？

eMall 订单服务连接池耗尽时，先看是否下单事务里调用了库存、支付或风控远程服务。

如果远程调用在事务内，连接会长时间占用。应调整为先校验，再开启短事务写订单和 Outbox。

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
数据库连接池耗尽说明连接被占满或获取连接太慢。排查要看活跃连接、等待线程、获取连接耗时、慢 SQL、
长事务和数据库资源。

不能只调大连接池。更重要的是缩短事务、优化慢 SQL、避免事务内远程调用、修复连接泄露并限流保护
数据库。
```

## 回答评分点

高分答案应该覆盖：

- 看活跃连接和等待连接。
- 慢 SQL 和长事务是重点。
- 连接泄露要排查。
- 不要盲目调大连接池。
- 事务内远程调用风险大。

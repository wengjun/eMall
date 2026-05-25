# 445 Redis 命中率下降如何排查？

[返回按分类学习面试题](../README.md)

## 题目

Redis 命中率下降如何排查？

## 先给面试官的短答案

Redis 命中率下降要按业务前缀、接口、实例和时间线拆分，检查 key 生成逻辑、TTL、批量删除、内存
淘汰、缓存预热、流量变化、穿透攻击和发布变更。

命中率下降的最大风险是数据库回源压力升高。

## 排查维度

维度：

- 哪个 key 前缀下降。
- 哪个接口下降。
- 哪个 Redis 实例下降。
- 是否发布新版本。
- 是否活动开始。
- 是否出现大量不存在 key。

不要只看全局命中率。

## 常见原因

原因：

- key 命名变更。
- TTL 设置过短。
- TTL 同时过期。
- 批量删除缓存。
- Redis 内存淘汰。
- 预热失败。
- 缓存穿透。
- 热点数据切换。

发布和活动最容易触发命中率变化。

## 应急处理

处理：

- 降低回源并发。
- 热点 key 预热。
- 临时延长 TTL。
- 恢复 key 兼容。
- 启用空值缓存。
- 启用布隆过滤器。
- 保护数据库限流。

先保护数据库，再修复根因。

## 在 eMall 项目中怎么讲？

eMall 商品详情命中率突然下降时，先看 `product:detail` 前缀。如果新版本把 key 版本从 v1 改成 v2，
会导致缓存整体未命中。

应做兼容读取或分批预热，避免商品库被全量回源打爆。

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
Redis 命中率下降要按 key 前缀、接口、实例和时间线排查。常见原因是发布改变 key、TTL 过短、批量
删除、内存淘汰、预热失败、穿透攻击和热点切换。

处理时优先保护数据库，限制回源并发，预热热点 key，修复 key 逻辑，补充空值缓存和布隆过滤器。
```

## 回答评分点

高分答案应该覆盖：

- 不只看全局命中率。
- 按 key 前缀和接口拆分。
- key 变更、TTL、淘汰和预热。
- 穿透会降低命中率。
- 应急先保护数据库。

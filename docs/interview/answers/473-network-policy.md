# 473 NetworkPolicy 解决什么问题？

[返回按分类学习面试题](../README.md)

## 题目

NetworkPolicy 解决什么问题？

## 先给面试官的短答案

NetworkPolicy 解决 Kubernetes 集群内 Pod 间网络访问控制问题。默认情况下，很多集群中的 Pod 可以互相访问，
这会扩大横向移动和误调用风险。NetworkPolicy 可以按 namespace、Pod label、端口和方向限制入站与出站流量，
让服务只访问它应该访问的依赖。

## 为什么需要 NetworkPolicy

微服务越多，网络边界越重要。如果订单服务被攻击者控制，而集群网络默认全通，攻击者可能继续访问支付、
风控、数据库代理、内部运维接口或管理面服务。NetworkPolicy 的目标是把“默认全通”改成“最小权限访问”。

它不能替代认证鉴权，但能降低网络层暴露面。即使某个服务存在漏洞，攻击者也难以随意横向移动。

## 核心概念

NetworkPolicy 通常包含：

- `podSelector`：选择策略作用的目标 Pod。
- `ingress`：控制谁可以访问这些 Pod。
- `egress`：控制这些 Pod 可以访问谁。
- `namespaceSelector`：按命名空间筛选来源或目标。
- `podSelector`：按 Pod 标签筛选来源或目标。
- `ports`：限制协议和端口。

需要注意，NetworkPolicy 是否生效取决于网络插件。Calico、Cilium 等 CNI 支持较完整的策略能力。

## 生产设计原则

生产环境建议从核心服务开始做默认拒绝，再逐步放开必要依赖。例如支付服务只允许网关、订单服务和回调处理器访问，
并只允许访问风控、账务、消息代理和必要的外部出口。

策略上线要灰度验证，避免一次性收紧导致生产中断。可以先观察流量，再生成候选策略，最后分批启用。

## 在 eMall 项目中怎么讲？

eMall 可以按域划分 namespace，例如交易、支付、商品、数据平台和运维平台。订单服务允许访问库存、促销、
支付和消息平台，但不应该直接访问数据仓库或内部审批服务。支付服务的入站来源应更严格，出站也要限制到必要系统。

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
NetworkPolicy 是 Kubernetes 的网络最小权限机制。它用 label 和 namespace 表达哪些 Pod 可以访问哪些 Pod，
以及允许哪些端口和方向。

在生产环境中，我不会只依赖应用层鉴权。应用层鉴权解决“你是谁、能不能做某个动作”，
NetworkPolicy 解决“你从网络上能不能连到这个服务”。两者结合才能减少横向移动风险。

落地时要注意三点：第一，确认 CNI 支持策略；第二，先对高风险服务启用默认拒绝；
第三，用观测数据验证真实依赖，避免误封流量。对电商系统来说，支付、风控、开放平台和运维接口应该优先收紧。
```

## 回答评分点

高分答案应该覆盖：

- 说明 NetworkPolicy 控制 Pod 间入站和出站访问。
- 知道默认全通带来的横向移动风险。
- 能讲清 selector、namespace、端口和方向。
- 知道策略依赖 CNI 支持。
- 能结合电商支付、风控、运维接口做最小权限设计。

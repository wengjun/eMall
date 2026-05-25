# 461 Kubernetes Deployment、Service、Ingress 分别是什么？

[返回按分类学习面试题](../README.md)

## 题目

Kubernetes Deployment、Service、Ingress 分别是什么？

## 先给面试官的短答案

Deployment 管理 Pod 副本和滚动发布，Service 为一组 Pod 提供稳定访问入口和负载均衡，Ingress 负责
把集群外部 HTTP/HTTPS 流量按域名和路径路由到 Service。

简单说，Deployment 管实例，Service 管内部访问，Ingress 管外部入口。

## Deployment

Deployment 负责：

- 管理副本数。
- 滚动升级。
- 回滚。
- 自愈重建 Pod。
- 维护期望状态。

应用版本发布通常通过 Deployment 完成。

## Service

Service 负责：

- 提供稳定虚拟 IP。
- 通过 label selector 关联 Pod。
- 对 Pod 做负载均衡。
- 屏蔽 Pod IP 变化。

Pod 会变化，Service 名称和地址相对稳定。

## Ingress

Ingress 负责：

- 暴露 HTTP/HTTPS。
- 域名路由。
- 路径路由。
- TLS 终止。
- 流量进入集群。

Ingress 需要 Ingress Controller 实际执行。

## 在 eMall 项目中怎么讲？

eMall 的 `order` 服务用 Deployment 部署多个 Pod，用 Service 暴露集群内稳定地址。

外部用户请求先到 Ingress 或 API Gateway，再路由到 gateway 服务，最终由网关转发到订单、商品和
支付等内部 Service。

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
Deployment 管理应用副本、滚动升级和回滚；Service 给一组 Pod 提供稳定访问入口和负载均衡；
Ingress 把集群外 HTTP/HTTPS 流量按域名和路径路由到 Service。

它们解决的是不同层次的问题：Deployment 管工作负载生命周期，Service 管服务发现和内部负载均衡，
Ingress 管外部入口。
```

## 回答评分点

高分答案应该覆盖：

- Deployment 管 Pod 副本和发布。
- Service 提供稳定访问入口。
- Ingress 管外部 HTTP 入口。
- Ingress 需要 Controller。
- 能结合微服务访问链路说明。

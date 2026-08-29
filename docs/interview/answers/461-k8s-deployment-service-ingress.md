# 461 Kubernetes Deployment、Service、Ingress 分别是什么？

[返回按分类学习面试题](../README.md)

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

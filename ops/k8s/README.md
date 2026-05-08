# Kubernetes 基线

[项目首页](../../README.md) | [文档索引](../../docs/README.md) | [运维配置索引](../README.md)

`ops/k8s` 目录提供 eMall 在线服务的 Kubernetes 部署基线。它用于表达生产部署需要考虑的资源和策略，
不是可以不修改就直接上线的最终清单。

## 已包含内容

- Deployment。
- Service。
- PodDisruptionBudget。
- HorizontalPodAutoscaler。
- 健康检查和就绪检查。
- 资源 requests 和 limits。
- 优雅关闭配置。
- ServiceAccount。
- NetworkPolicy。
- 安全上下文。
- TLS ingress 示例。
- 混沌工程示例。
- 灰度发布和服务网格相关示例。

## 建议应用顺序

1. 先创建 namespace、Secret、ConfigMap 和 ServiceAccount。
2. 再部署 MySQL、Redis、Kafka、OpenSearch 等基础设施，或者接入外部托管服务。
3. 部署核心服务：gateway、user、product、inventory、order、payment。
4. 部署扩展服务：search、fulfillment、review、after-sales、merchant 等。
5. 部署观测组件和告警规则。
6. 最后再启用 HPA、PDB、NetworkPolicy、灰度和混沌演练。

## 生产前必须修改

- 镜像仓库和镜像 tag。
- 域名、TLS 证书和 Ingress。
- Secret、数据库密码、内部运维 token。
- 资源 requests/limits。
- HPA 指标和阈值。
- 探针路径和超时时间。
- 网络策略白名单。
- 日志、指标和 trace 后端地址。

## 注意事项

- 不要把本地开发密码直接用于 Kubernetes。
- 不要在没有容量验证的情况下启用自动扩容。
- 不要在真实生产直接执行混沌清单。
- 不要让内部运维接口暴露到公网。

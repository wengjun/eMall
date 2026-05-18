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
- Kubernetes Gateway API HTTPS/TLS 入口、HTTP 强制跳转和 HSTS 示例。
- 混沌工程示例。
- 灰度发布和服务网格相关示例。

## 建议应用顺序

1. 先安装 Gateway API CRD 和云厂商 ALB 控制器，确认集群存在可用的 `GatewayClass`。
2. 创建 namespace、Secret、ConfigMap 和 ServiceAccount。
3. 再部署 MySQL、Redis、Kafka、Nacos、Elasticsearch 等基础设施，或者接入外部托管服务。
4. 部署核心服务：gateway、user、product、inventory、order、payment。
5. 部署 `gateway-api.yml`，暴露公网 HTTPS 入口。
6. 部署扩展服务：search、fulfillment、review、after-sales、merchant 等。
7. 部署观测组件和告警规则。
8. 最后再启用 HPA、PDB、NetworkPolicy、灰度和混沌演练。

## 生产前必须修改

- 镜像仓库和镜像 tag。
- 域名、TLS 证书、GatewayClass 和 ALB 控制器。
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
- Web 和手机 App 的公网请求只暴露 `https://api.emall.example.com`，TLS 在 Kubernetes Gateway API + 云厂商 ALB 层终止，内部继续转发到 `gateway:8080`。

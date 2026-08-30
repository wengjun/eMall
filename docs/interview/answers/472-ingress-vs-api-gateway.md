# 472 Ingress 和 API Gateway 有什么区别？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Ingress 是 Kubernetes 对集群入口七层路由的标准抽象，主要负责把外部 HTTP 流量按域名和路径转发到 Service。
API Gateway 是面向业务 API 的统一入口，除了路由，还负责认证、鉴权、限流、签名、灰度、协议转换、
审计、聚合和开放平台治理。Ingress 更偏基础设施入口，API Gateway 更偏业务流量治理。

## Ingress 负责什么

Ingress 常见能力包括：

- 基于 host 和 path 路由到不同 Service。
- TLS 证书终止。
- 基础重写、重定向和负载均衡。
- 对接云厂商负载均衡器或 Nginx、Traefik、Envoy 等控制器。

Ingress 的核心价值是让 Kubernetes 内部服务能以统一方式暴露给集群外部。

## API Gateway 负责什么

API Gateway 常见能力包括：

- 统一认证、鉴权、租户识别和用户上下文透传。
- 限流、熔断、黑白名单、风险控制和防刷。
- HMAC 签名、nonce 防重放、请求体 hash 校验。
- API 版本管理、灰度发布、协议转换和结果包装。
- 访问日志、审计、计量计费、开放平台 appKey 管理。

API Gateway 面向业务 API 生命周期治理，通常是所有外部调用进入后端微服务前的第一道业务防线。

## 两者如何协作

常见架构是：公网负载均衡器先把流量转到 Ingress，Ingress 再转发到 API Gateway，API Gateway 完成业务治理后，
再调用内部服务。也可以让 API Gateway 直接作为 Ingress Controller，但要避免职责混乱。

如果只是内部系统入口，Ingress 可能已经足够。如果面向 App、小程序、商家、开放平台和第三方系统，
就需要 API Gateway 统一处理安全、流控和审计。

## 电商系统实践

大型电商系统适合把 `openapi`、`identity`、`risk`、`traffic` 等能力放在 API Gateway 或网关层。
Ingress 负责域名和证书入口，API Gateway 负责用户态和业务态控制，例如登录态校验、商家接口签名、
接口限流、灰度路由和审计日志。

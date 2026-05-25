# 472 Ingress 和 API Gateway 有什么区别？

[返回按分类学习面试题](../README.md)

## 题目

Ingress 和 API Gateway 有什么区别？

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

## 在 eMall 项目中怎么讲？

eMall 适合把 `openapi`、`identity`、`risk`、`traffic` 等能力放在 API Gateway 或网关层。
Ingress 负责域名和证书入口，API Gateway 负责用户态和业务态控制，例如登录态校验、商家接口签名、
接口限流、灰度路由和审计日志。

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
Ingress 和 API Gateway 都能转发流量，但抽象层次不同。Ingress 是 Kubernetes 的入口资源，
重点是把外部 HTTP 请求路由到集群内 Service。API Gateway 是业务 API 的治理平面，
重点是让 API 可安全、可控、可观测地对外提供。

我通常不会把所有能力都塞进 Ingress。Ingress 负责基础入口，API Gateway 负责认证鉴权、
限流防刷、签名验签、灰度、审计、版本管理和开放平台治理。

在电商系统中，用户下单、支付、商家改价、开放平台回调都需要强业务治理。
这些能力如果分散在每个服务里，会导致重复实现和安全策略不一致，所以应该沉到网关层。
```

## 回答评分点

高分答案应该覆盖：

- 说明 Ingress 是 Kubernetes 入口路由抽象。
- 说明 API Gateway 是业务 API 治理入口。
- 能列出 API Gateway 的认证、鉴权、限流、签名、审计能力。
- 知道两者可以串联，也可以部分融合。
- 能指出不要把业务安全逻辑全部下沉到 Ingress 注解里。

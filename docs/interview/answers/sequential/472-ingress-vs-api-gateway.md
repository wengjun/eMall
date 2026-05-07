# 472 Ingress 和 API Gateway 有什么区别？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

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

![Kubernetes 生产运行和故障治理](../../assets/kubernetes-operations.svg)

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

## 深度完善：面向 L6 的回答框架

围绕「Ingress 和 API Gateway 有什么区别？」，高分答案不能停在概念定义，而要把「镜像、Probe、HPA、资源限制、滚动升级、灰度、回滚、多 AZ 和 Service Mesh」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「容器、Kubernetes 和发布」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `ops/k8s、release、platform-ops、gateway 和核心业务服务的部署清单` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：Pod 重启数、Probe 失败数、CPU throttle、HPA 扩缩容次数、灰度错误率。
- 验证证据：Deployment、PDB、HPA、灰度记录、回滚记录、资源曲线和演练报告。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：Ingress 和 API Gateway 有什么区别？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

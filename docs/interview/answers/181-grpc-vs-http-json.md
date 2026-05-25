# 181 gRPC 和 HTTP JSON 如何取舍？

[返回按分类学习面试题](../README.md)

## 题目

gRPC 和 HTTP JSON 如何取舍？

## 先给面试官的短答案

HTTP JSON 适合开放 API、浏览器和跨语言接入，生态成熟、调试简单、兼容性好。
gRPC 适合内部高性能服务调用，强契约、低序列化开销、支持流式通信，但对网关、调试和治理要求更高。

电商系统通常对外用 HTTP JSON，对内核心服务可按需使用 gRPC 或其他 RPC。

## HTTP JSON 的特点

优点：

- 可读性好。
- 浏览器和网关支持成熟。
- 调试简单。
- 适合开放平台。
- 兼容性治理容易理解。

不足：

- JSON 序列化体积较大。
- 类型约束弱。
- 对高频内部调用有额外开销。

## gRPC 的特点

优点：

- 使用 Protobuf 强类型契约。
- 序列化体积小。
- 性能较好。
- 支持双向流。
- 适合内部服务间调用。

不足：

- 浏览器直接接入不如 HTTP JSON 方便。
- 调试门槛更高。
- 需要治理 `.proto` 兼容性。
- 网关、鉴权、观测和限流要配套建设。

## 取舍标准

选择时看：

- 是否面向外部开发者。
- 是否需要浏览器直接调用。
- 调用频率和延迟要求。
- 契约是否需要强类型。
- 是否需要流式通信。
- 团队是否具备治理能力。

技术选择不能只看性能，还要看运维和协作成本。

## 在 eMall 项目中怎么讲？

开放平台给商家 ERP 的接口应使用 HTTP JSON，便于签名、文档、调试和跨语言接入。

内部库存、价格、推荐等高频服务可以评估 gRPC，但必须配套 deadline、重试、熔断、限流、链路追踪和协议兼容治理。

## 深度增强：可观测与配置治理图

![指标、日志、Trace 和告警平台](../assets/observability-platform.svg)

配置、日志、指标和 Trace 不是附属能力，而是生产系统定位问题和控制变更风险的基础。
没有可观测性，限流、熔断、回滚和补偿都很难判断是否有效。

## 深度增强：Java 17 观测信号示例

```java
import java.time.Instant;
import java.util.Map;

record ObservabilityEvent(
        Instant time,
        String traceId,
        String service,
        String eventType,
        Map<String, String> tags) {
}

final class TraceTagPolicy {

    boolean shouldKeep(String key) {
        return !key.equalsIgnoreCase("password")
                && !key.equalsIgnoreCase("secret")
                && !key.equalsIgnoreCase("token");
    }
}
```

这段代码体现生产观测的两个重点：所有关键事件要能关联 traceId，敏感信息不能进入日志和标签。

## 深度增强：生产边界

日志越多不代表越好。核心链路要控制日志成本、采样率、脱敏和索引字段。告警也不能只看机器指标，
还要看下单成功率、支付成功率、库存失败率、Outbox 积压和用户投诉。

## 深度增强：面试高分表达

我会把可观测性讲成故障闭环：指标发现异常，Trace 定位慢在哪里，日志解释发生了什么，
告警和 Runbook 指导恢复。配置变更也要有版本、审批、灰度、审计和回滚，避免配置事故变成全站事故。

## 专家级完整回答

```text
HTTP JSON 更适合开放接口和浏览器场景，生态成熟、调试简单、兼容性成本低。gRPC 更适合内部高频调用，
它通过 Protobuf 提供强契约和更低序列化成本，并支持流式通信。

我会对外优先 HTTP JSON，对内在高频低延迟场景评估 gRPC。但 gRPC 必须配套超时、熔断、限流、追踪和 proto 兼容治理。
```

## 回答评分点

高分答案应该覆盖：

- HTTP JSON 适合开放和浏览器场景。
- gRPC 适合内部高频强契约调用。
- gRPC 需要治理 `.proto` 兼容。
- 性能不是唯一标准。
- 两者可以在不同边界共存。

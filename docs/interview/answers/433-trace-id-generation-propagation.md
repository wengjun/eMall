# 433 trace ID 如何生成和透传？

[返回按分类学习面试题](../README.md)

## 题目

trace ID 如何生成和透传？

## 先给面试官的短答案

trace ID 通常在请求入口生成，如果上游已经传入可信 trace ID，则继续使用。之后通过 HTTP header、
RPC metadata、消息 header 和日志上下文在整个链路中透传。

关键是入口统一生成、跨线程不丢、跨消息不丢、日志自动带上。

## 生成位置

生成位置：

- API Gateway。
- Web Filter。
- RPC 拦截器。
- 消息消费者入口。
- 定时任务入口。

所有入口都要有 trace ID。

## 透传方式

透传方式：

- HTTP 使用 `traceparent` 或 `X-Trace-Id`。
- RPC 使用 metadata。
- Kafka 使用 message headers。
- 异步线程使用上下文包装。
- 日志使用 MDC。

异步和消息场景最容易丢失 trace ID。

## 注意点

注意：

- 外部传入 trace ID 要校验长度和字符。
- 不可信来源不能无限制接受。
- trace ID 不应包含隐私信息。
- 日志、指标和 Trace 统一使用。
- 采样不影响日志字段记录。

trace ID 是排障索引，不是认证凭证。

## 在 eMall 项目中怎么讲？

eMall 下单请求从网关生成 trace ID，调用用户、商品、价格、库存、订单和支付服务时通过 header
透传。

订单服务发布 Kafka 事件时，把 trace ID 写入消息 header，库存消费者处理时继续写入日志 MDC。

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
trace ID 应在请求入口统一生成，并在 HTTP、RPC、消息和异步线程中持续透传。日志框架通过 MDC
自动把 trace ID 写入每条日志，Trace 系统用它串联 span。

生产中特别要处理异步线程池、Kafka 消息和定时任务入口，否则链路会断。外部传入的 trace ID 要
校验，避免日志污染。
```

## 回答评分点

高分答案应该覆盖：

- 入口生成 trace ID。
- HTTP、RPC、消息都要透传。
- 日志 MDC 自动带上。
- 异步线程和消息容易丢。
- 外部 trace ID 要校验。

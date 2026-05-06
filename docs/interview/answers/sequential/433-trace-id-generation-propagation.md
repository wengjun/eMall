# 433 trace ID 如何生成和透传？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

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

## 深度完善：面向 L6 的回答框架

围绕「trace ID 如何生成和透传？」，高分答案不能停在概念定义，而要把「日志、指标、Trace、SLO、告警、Runbook、事故复盘和故障演练」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「可观测性和 SRE」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `observability、analytics、operations、gateway、order、payment 的核心看板` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：RED/USE 指标、SLO、错误预算、告警准确率、MTTD、MTTR。
- 验证证据：Trace 样例、指标看板、告警规则、Runbook、事故时间线和复盘行动项。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：trace ID 如何生成和透传？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。


# 434 日志中必须包含哪些业务字段？

[返回按分类学习面试题](../README.md)

## 题目

日志中必须包含哪些业务字段？

## 先给面试官的短答案

日志字段要能支持排障、审计和业务追溯。电商核心日志至少应包含 trace ID、用户 ID、订单号、
请求入口、接口名、结果码、耗时、关键业务状态和错误原因摘要。

字段要足够定位问题，但不能写入敏感明文。

## 通用字段

通用字段：

- trace ID。
- span ID。
- service name。
- instance ID。
- environment。
- timestamp。
- log level。
- request path。

这些字段支持技术排障。

## 业务字段

业务字段：

- userId。
- orderNo。
- paymentNo。
- skuId。
- merchantId。
- idempotencyKey。
- businessStatus。
- resultCode。

具体字段要按业务域选择。

## 禁止内容

不能记录：

- 完整手机号。
- 身份证号。
- 银行卡。
- token。
- secret。
- 完整地址。
- 密码或验证码。

敏感字段要脱敏或删除。

## 在 eMall 项目中怎么讲？

eMall 下单日志应记录 trace ID、userId、orderNo、skuId 列表摘要、幂等号、订单状态、结果码和耗时。

不要记录完整收货地址、手机号和支付凭证。排障需要的是关联字段，不是用户隐私。

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
日志字段要支持定位问题和业务追溯。通用上要有 trace ID、服务名、实例、环境、接口、结果码和耗时；
业务上要有 userId、orderNo、paymentNo、skuId、merchantId 和幂等号等关键标识。

同时日志必须做数据最小化，手机号、地址、token、secret 和支付信息不能明文写入。
```

## 回答评分点

高分答案应该覆盖：

- trace ID 必须有。
- 业务关键 ID 必须有。
- 结果码和耗时要记录。
- 字段服务于排障和审计。
- 敏感信息不能明文记录。

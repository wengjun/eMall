# 450 SLO、SLI、SLA 有什么区别？

[返回按分类学习面试题](../README.md)

## 题目

SLO、SLI、SLA 有什么区别？

## 先给面试官的短答案

SLI 是服务水平指标，例如可用性、延迟和错误率。SLO 是内部目标，例如 99.9% 下单成功率。SLA 是
对客户或业务方承诺的协议，通常带有赔付或责任约束。

简单说，SLI 是测量值，SLO 是目标，SLA 是承诺。

## SLI

SLI 示例：

- 请求成功率。
- P99 延迟。
- 可用性。
- 消息延迟。
- 数据新鲜度。
- 订单成功率。

SLI 必须可测量。

## SLO

SLO 示例：

- 下单接口 99.95% 请求成功。
- 支付回调 P99 小于 1 秒。
- 搜索索引 99% 在 1 分钟内同步。
- 核心接口月可用性 99.9%。

SLO 是团队内部工程目标。

## SLA

SLA 特点：

- 面向外部客户或业务方。
- 是正式承诺。
- 可能包含赔付。
- 通常低于或等于内部 SLO。

不能轻易承诺无法测量或无法保障的 SLA。

## 在 eMall 项目中怎么讲？

eMall 可以定义下单成功率为 SLI，目标 99.95% 为 SLO。

如果开放平台向商家承诺接口月可用性 99.9%，并写入合同或服务协议，那就是 SLA。

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
SLI 是测量指标，SLO 是服务目标，SLA 是对外承诺。比如下单成功率是 SLI，月度成功率不低于 99.95%
是 SLO，对商家承诺接口可用性并有赔付条款是 SLA。

工程团队通常用 SLO 管理可靠性，用错误预算指导发布和稳定性投入。SLA 应谨慎承诺，并建立在可测量
的 SLI 之上。
```

## 回答评分点

高分答案应该覆盖：

- SLI 是指标。
- SLO 是目标。
- SLA 是承诺。
- SLA 通常面向外部并可能赔付。
- 能给出电商例子。

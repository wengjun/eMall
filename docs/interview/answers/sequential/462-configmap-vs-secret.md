# 462 ConfigMap 和 Secret 有什么区别？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

ConfigMap 和 Secret 有什么区别？

## 先给面试官的短答案

ConfigMap 用于保存非敏感配置，Secret 用于保存密码、token、证书等敏感配置。两者都可以通过环境
变量或文件挂载给 Pod 使用，但 Secret 需要更严格的访问控制、加密存储和轮换机制。

Secret 不是天然绝对安全，默认只是更适合敏感数据的 Kubernetes 对象。

## ConfigMap

适合：

- 普通配置项。
- 功能开关。
- 日志级别。
- 外部服务地址。
- 非敏感业务参数。

ConfigMap 不应保存密码和密钥。

## Secret

适合：

- 数据库密码。
- API token。
- TLS 证书。
- app secret。
- 私有仓库凭证。

Secret 要配合 RBAC 和 etcd 加密。

## 注意点

注意：

- Secret base64 不是加密。
- 限制谁能读取 Secret。
- 避免通过环境变量泄露。
- 支持密钥轮换。
- 不把 Secret 写入镜像。

敏感配置要全链路保护。

## 在 eMall 项目中怎么讲？

eMall 的日志级别、限流阈值可以放 ConfigMap。

数据库密码、开放平台签名 secret、支付通道证书应放 Secret，并限制只有对应服务账号能读取。

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
ConfigMap 保存非敏感配置，Secret 保存敏感数据。两者都能挂载到 Pod，但 Secret 需要 RBAC、etcd
加密、审计和轮换。

不要误以为 Secret 的 base64 就是安全。生产中还要避免 Secret 进入镜像、日志和环境变量泄露，并
按服务最小权限授权。
```

## 回答评分点

高分答案应该覆盖：

- ConfigMap 放非敏感配置。
- Secret 放敏感配置。
- Secret base64 不是加密。
- 需要 RBAC 和 etcd 加密。
- Secret 要支持轮换。

## 深度完善：面向 L6 的回答框架

围绕「ConfigMap 和 Secret 有什么区别？」，高分答案不能停在概念定义，而要把「镜像、Probe、HPA、资源限制、滚动升级、灰度、回滚、多 AZ 和 Service Mesh」讲成一条可验证的工程链路。
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

本题复习重点：ConfigMap 和 Secret 有什么区别？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

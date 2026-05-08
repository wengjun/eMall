# 536 设计配置中心

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

设计配置中心。

## 先给面试官的短答案

配置中心负责集中管理应用配置，支持环境隔离、版本管理、灰度发布、实时推送、权限审批、审计、回滚和客户端容错。
生产配置变更要像代码发布一样治理，不能直接全量推送危险配置。

## 核心能力

配置中心要支持应用、环境、集群、命名空间和配置项管理。每次变更要有版本、diff、变更人、审批人和原因。

发布能力要支持全量发布、灰度发布、按实例发布和快速回滚。高风险配置需要审批。

客户端要支持拉取、长轮询或推送、本地缓存、默认值、配置校验和失败降级。

## 数据模型

配置可以按 app、env、cluster、namespace、key 组织。配置版本表记录每次变更，发布表记录哪个版本发布到哪些实例。

审计表记录读取、修改、发布、回滚和审批。

## 高可用设计

配置中心服务要多副本部署，存储层高可用。客户端必须有本地快照，配置中心不可用时服务仍能使用最近一次成功配置启动。

配置推送失败时，客户端要能定期拉取兜底。配置解析失败时不能用坏配置覆盖好配置。

## 在 eMall 项目中怎么讲？

eMall 的支付通道权重、限流阈值、风控规则开关、灰度比例和降级开关都适合进入配置中心。
`governance` 和 `release` 可以承载配置治理，`operations` 提供审批和审计。

## 深度增强：配置发布闭环图

![配置中心和灰度发布闭环](../assets/config-release-loop.svg)

配置中心不是简单的 key-value 存储，而是动态配置发布平台。
核心交易配置变更必须像代码发布一样有 diff、审批、灰度、指标观察、审计和回滚。

## 深度增强：Java 17 配置模型

```java
public record ConfigKey(
        String app,
        String env,
        String cluster,
        String namespace,
        String key) {
}

public record ConfigVersion(
        ConfigKey key,
        String value,
        long version,
        String operator,
        String reason,
        Instant createdAt) {
}

public record ConfigRelease(
        ConfigKey key,
        long version,
        String targetExpression,
        int percentage,
        Instant releasedAt) {
}
```

客户端要保护最后一次有效配置：

```java
public final class SafeConfigClient {

    private final ConfigRemoteClient remoteClient;
    private final LocalConfigSnapshot snapshot;

    public String get(ConfigKey key, String defaultValue) {
        try {
            String value = remoteClient.get(key);
            snapshot.save(key, value);
            return value;
        } catch (RuntimeException ex) {
            return snapshot.get(key).orElse(defaultValue);
        }
    }
}
```

## 深度增强：生产边界

- 坏配置不能覆盖最后一次有效配置。
- 高风险配置要审批和灰度，不能全量推送。
- 配置要做类型、范围、语法和业务校验。
- 客户端要有本地快照，配置中心故障不应影响服务启动。
- 配置读取、修改、发布和回滚都要审计。

## 深度增强：面试高分表达

```text
我会把配置中心设计成发布系统，而不是配置表。每次变更有版本、diff、审批、灰度和回滚。
客户端使用长轮询或推送获取配置，但必须有本地快照和最后一次有效配置。
核心交易配置变更后要观察下单成功率、支付成功率和错误率，避免配置事故扩大。
```

## 专家级完整回答

```text
配置中心本质是动态配置的发布平台。

我会设计配置版本、灰度发布、审批、审计和快速回滚。客户端要有本地缓存和安全默认值，
不能因为配置中心不可用就让业务服务启动失败。

对核心交易配置，发布前要做语法和范围校验，发布后观察指标。配置错误导致的事故非常常见，
所以配置变更必须纳入发布治理。
```

## 回答评分点

高分答案应该覆盖：

- 覆盖环境、命名空间、版本、发布和回滚。
- 知道配置变更要审批和审计。
- 能说明客户端缓存、默认值和坏配置保护。
- 支持灰度发布和按实例发布。
- 能结合限流、风控、支付通道配置说明。
## 深度完善：专项验收清单

围绕「设计配置中心」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
回答时要把概念、代码、数据、失败路径和指标串起来，证明自己不是只理解单点知识。

### 项目落点

- 先说明它在 eMall 哪个模块或链路中出现，例如交易、库存、支付、搜索、风控、发布或可观测性。
- 再说明它保护的核心目标：正确性、可用性、延迟、成本、安全或协作效率。
- 最后补失败场景：超时、重试、重复请求、状态不一致、热点流量、配置错误或发布回滚。

### 验收证据

- 代码证据：关键类、状态机、唯一约束、事务边界、线程池隔离或配置项。
- 测试证据：单元测试、集成测试、契约测试、压测、故障注入或回归用例。
- 运行证据：指标看板、Trace、结构化日志、告警、Runbook、对账结果或补偿记录。

### 高分收束

面试最后要回到取舍：当前方案为什么足够简单可靠，什么时候需要升级，升级时如何灰度、回滚和验证。
这样回答能体现生产系统判断力，而不是只罗列技术名词。

深度完善标记：专题增强答案已补项目落点、验收证据和取舍收束。

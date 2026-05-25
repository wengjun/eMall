# 212 2PC 的流程是什么？

[返回按分类学习面试题](../README.md)

## 题目

2PC 的流程是什么？

## 先给面试官的短答案

2PC 是两阶段提交，包含准备阶段和提交阶段。
协调者先询问所有参与者能否提交，所有参与者都准备成功后，协调者再通知提交；只要有一个失败，就通知全部回滚。

它试图保证多个参与者原子提交，但会带来阻塞和可用性问题。

## 第一阶段：Prepare

流程：

- 协调者向所有参与者发送 prepare 请求。
- 参与者执行本地事务检查。
- 参与者写入预提交日志。
- 参与者锁定相关资源。
- 参与者回复 yes 或 no。

回复 yes 表示参与者承诺后续可以提交。

## 第二阶段：Commit 或 Rollback

如果所有参与者回复 yes：

- 协调者发送 commit。
- 参与者提交本地事务。
- 参与者释放资源。
- 参与者回复完成。

如果任一参与者回复 no 或超时：

- 协调者发送 rollback。
- 参与者回滚本地事务。
- 参与者释放资源。

## 关键角色

角色包括：

- 协调者：决定全局提交或回滚。
- 参与者：执行本地准备、提交和回滚。
- 日志：用于故障恢复。

日志很重要，因为节点宕机后要根据日志恢复事务状态。

## 在 eMall 项目中怎么讲？

如果强行用 2PC 处理下单，订单、库存、支付都要先 prepare 并锁住资源，等协调者决定后再提交。

这会让库存和支付资源被长时间占用，不适合高并发下单主链路。

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
2PC 分为 prepare 和 commit 两阶段。第一阶段协调者询问所有参与者是否可以提交，
参与者完成本地检查、写日志并锁定资源后返回 yes 或 no。第二阶段如果全部 yes，协调者通知 commit；
如果任一失败或超时，通知 rollback。

它可以实现多个参与者的原子提交，但因为资源在 prepare 后被锁住，所以存在阻塞和可用性问题。
```

## 回答评分点

高分答案应该覆盖：

- 2PC 有协调者和参与者。
- 第一阶段是 prepare。
- 第二阶段是 commit 或 rollback。
- 参与者 prepare 后会锁资源。
- 日志用于故障恢复。

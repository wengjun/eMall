# 276 如何设计下游自动平滑恢复？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

如何设计下游自动平滑恢复？

## 先给面试官的短答案

下游恢复后不能立即恢复全量流量，要通过熔断半开、小流量探测、预热、逐步放量、限速重试和指标观察实现平滑恢复。
恢复过程要自动化，并能在指标恶化时重新熔断或降级。

平滑恢复的目标是避免二次雪崩。

## 恢复步骤

步骤：

- 熔断打开保护调用方。
- 等待冷却窗口。
- 半开少量探测。
- 观察成功率和延迟。
- 逐步扩大流量。
- 恢复正常调用。
- 指标恶化时回退。

每个阶段都要有阈值。

## 预热内容

预热包括：

- 连接池。
- 线程池。
- JVM JIT。
- 本地缓存。
- Redis 缓存。
- 配置和规则。
- 数据库连接。

刚启动成功不等于具备承载高峰流量的能力。

## 重试恢复

下游恢复后，上游积压的重试和队列可能同时涌入。

要做：

- 重试限速。
- 队列消费限速。
- 分批恢复。
- 按优先级恢复。

否则恢复瞬间可能比故障前压力更大。

## 在 eMall 项目中怎么讲？

支付服务恢复后，订单服务先通过半开状态放少量支付查询请求。

同时限制支付补偿任务的重试速率，避免大量积压补偿请求把支付服务再次打挂。

## 深度增强：状态机图

![下游自动平滑恢复状态机](../assets/downstream-smooth-recovery.svg)

这张图的重点是：恢复不是从 `OPEN` 直接跳到 `CLOSED`。
生产系统里更安全的过程是 `OPEN -> HALF_OPEN -> WARMUP -> CLOSED`，中间任意阶段指标恶化都要回退。

## 深度增强：Java 17 代码实现

下面是一个简化版恢复控制器。它表达了三个关键能力：半开探测、预热放量、指标恶化回退。

```java
public enum RecoveryState {
    OPEN,
    HALF_OPEN,
    WARMUP,
    CLOSED
}

public record ProbeResult(boolean success, long latencyMillis) {
}

public record RecoverySnapshot(
        RecoveryState state,
        int allowedPermits,
        double errorRate,
        long p99LatencyMillis) {
}

public final class SmoothRecoveryController {

    private static final int MAX_PERMITS = 10_000;
    private static final int HALF_OPEN_PERMITS = 20;

    private RecoveryState state = RecoveryState.OPEN;
    private int allowedPermits = 0;

    public RecoverySnapshot onCooldownFinished() {
        state = RecoveryState.HALF_OPEN;
        allowedPermits = HALF_OPEN_PERMITS;
        return snapshot(0.0, 0);
    }

    public RecoverySnapshot onProbeWindowFinished(double errorRate, long p99LatencyMillis) {
        if (errorRate > 0.02 || p99LatencyMillis > 300) {
            state = RecoveryState.OPEN;
            allowedPermits = 0;
            return snapshot(errorRate, p99LatencyMillis);
        }

        if (state == RecoveryState.HALF_OPEN) {
            state = RecoveryState.WARMUP;
            allowedPermits = 100;
            return snapshot(errorRate, p99LatencyMillis);
        }

        if (state == RecoveryState.WARMUP) {
            allowedPermits = Math.min(MAX_PERMITS, allowedPermits * 2);
            if (allowedPermits == MAX_PERMITS) {
                state = RecoveryState.CLOSED;
            }
        }

        return snapshot(errorRate, p99LatencyMillis);
    }

    public boolean tryAcquirePermit() {
        if (state == RecoveryState.OPEN) {
            return false;
        }
        return allowedPermits > 0;
    }

    private RecoverySnapshot snapshot(double errorRate, long p99LatencyMillis) {
        return new RecoverySnapshot(state, allowedPermits, errorRate, p99LatencyMillis);
    }
}
```

真实系统中 `allowedPermits` 不应该只是一个普通整数，而应接入令牌桶、滑动窗口或网关动态限流。
如果有多个实例，还要通过配置中心或控制面统一下发恢复速率，避免每个实例都各自放量导致总流量过大。

## 深度增强：恢复指标

平滑恢复至少要看这些指标：

- 请求成功率：判断下游是否真正恢复。
- P95/P99 延迟：平均值正常不代表尾延迟正常。
- 连接池等待时间：下游刚恢复时连接池最容易被打满。
- 线程池队列长度：上游补偿任务可能堆积后同时恢复。
- 重试速率：恢复阶段最怕历史重试和新流量叠加。
- 下游 CPU、内存、GC、数据库连接：健康检查成功不代表容量已经恢复。

## 深度增强：面试高分表达

可以这样回答：

```text
我不会把健康检查成功等同于完全恢复。恢复过程要状态机化：先熔断保护调用方，
冷却后进入半开，只允许少量真实请求探测；探测通过后进入预热阶段，逐步增加令牌；
如果错误率、P99 或资源指标恶化，就自动回到熔断。与此同时，补偿任务和队列消费必须限速，
否则历史积压会在恢复瞬间形成第二次洪峰。
```

## 专家级完整回答

```text
下游自动平滑恢复要结合熔断半开、少量探测、预热、逐步放量和限速重试。
不能因为健康检查成功就立即放开全部流量，因为缓存、连接池和积压请求可能导致二次雪崩。

恢复过程要看成功率、P99 延迟、错误率和资源指标，异常时自动回到熔断或降级。
```

## 回答评分点

高分答案应该覆盖：

- 恢复不能立即全量放开。
- 半开探测和逐步放量是核心。
- 预热很重要。
- 积压重试要限速。
- 指标恶化要自动回退。
## 深度完善：专项验收清单

围绕「如何设计下游自动平滑恢复？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

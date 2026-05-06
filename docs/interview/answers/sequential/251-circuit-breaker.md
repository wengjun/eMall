# 251 什么是熔断？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

什么是熔断？

## 先给面试官的短答案

熔断是在下游持续失败或变慢时，调用方临时停止继续请求下游，直接快速失败或走降级逻辑。
它的目标是保护调用方资源，避免线程、连接和请求在故障下游上持续堆积，最终引发级联故障。

熔断不是解决下游故障，而是阻止故障继续扩散。

## 为什么需要熔断？

没有熔断时：

- 下游超时导致调用方线程被占满。
- 连接池被耗尽。
- 重试继续打向故障服务。
- 上游请求排队增加。
- 故障沿调用链扩散。

熔断通过快速失败释放资源，让系统保持局部可用。

## 熔断依据

常见依据：

- 错误率超过阈值。
- 慢调用比例超过阈值。
- 连续失败次数超过阈值。
- 并发请求数超过阈值。
- 下游返回限流或过载信号。

阈值要结合服务等级和压测结果设置。

## 熔断后的行为

熔断后可以：

- 快速返回错误。
- 返回缓存兜底。
- 返回默认值。
- 隐藏非核心模块。
- 进入排队或稍后重试。

具体行为取决于业务是否允许降级。

## 在 eMall 项目中怎么讲？

推荐服务故障时，商品详情页可以熔断推荐调用，返回热门商品或隐藏推荐模块。

这样推荐故障不会拖慢商品详情页，也不会进一步影响下单链路。

## 深度增强：熔断和限流的关系图

![限流和熔断在调用链中的位置](../../assets/rate-limit-circuit-breaker.svg)

限流发生在“流量超过承载能力”时，重点是防止过多请求进入系统。
熔断发生在“下游已经异常”时，重点是阻止调用方继续把线程、连接和重试浪费在故障下游上。

## 深度增强：Java 17 熔断器状态机

下面是简化版熔断器。核心是三态：`CLOSED` 正常调用，`OPEN` 快速失败，`HALF_OPEN` 小流量探测恢复。

```java
public enum CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
}

public final class CircuitBreaker {

    private final int failureThreshold;
    private final Duration openDuration;
    private CircuitState state = CircuitState.CLOSED;
    private int consecutiveFailures;
    private Instant openedAt = Instant.EPOCH;

    public CircuitBreaker(int failureThreshold, Duration openDuration) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
    }

    public synchronized boolean allowRequest(Instant now) {
        if (state == CircuitState.CLOSED) {
            return true;
        }
        if (state == CircuitState.OPEN && now.isAfter(openedAt.plus(openDuration))) {
            state = CircuitState.HALF_OPEN;
            return true;
        }
        return state == CircuitState.HALF_OPEN;
    }

    public synchronized void recordSuccess() {
        consecutiveFailures = 0;
        state = CircuitState.CLOSED;
    }

    public synchronized void recordFailure(Instant now) {
        consecutiveFailures++;
        if (state == CircuitState.HALF_OPEN || consecutiveFailures >= failureThreshold) {
            state = CircuitState.OPEN;
            openedAt = now;
        }
    }

    public synchronized CircuitState state() {
        return state;
    }
}
```

真实生产系统不会只用连续失败次数，还会引入滑动窗口错误率、慢调用比例、最小请求数和半开探测并发数。
否则低流量服务可能误熔断，高流量服务可能发现太慢。

## 深度增强：面试高分表达

```text
熔断是调用方的自我保护，不是修复下游。它依赖超时和指标统计，当错误率、慢调用比例或连续失败超过阈值时，
进入 OPEN 状态快速失败或走降级；冷却窗口后进入 HALF_OPEN，只放少量探测请求；
如果探测成功再关闭熔断，否则继续打开。它要和线程池隔离、限流、降级、重试退避一起使用。
```

## 专家级完整回答

```text
熔断是调用方保护机制。当下游错误率、慢调用比例或连续失败超过阈值时，调用方临时停止请求下游，
直接快速失败或走降级逻辑，避免线程池、连接池和请求队列被故障下游拖垮。

熔断的价值是隔离故障和防止级联扩散。它需要配合超时、限流、降级和半开探测使用。
```

## 回答评分点

高分答案应该覆盖：

- 熔断用于防止故障扩散。
- 熔断通常在调用方生效。
- 错误率和慢调用比例是常见触发条件。
- 熔断后要快速失败或降级。
- 熔断要配合半开探测恢复。

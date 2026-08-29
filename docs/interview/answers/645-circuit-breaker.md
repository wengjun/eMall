# 645 手写熔断器状态机。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

enum CircuitState {
    CLOSED, OPEN, HALF_OPEN
}

record CircuitPermit(long generation) {
}

final class CircuitBreaker {
    private final int failureThreshold;
    private final Duration openDuration;
    private CircuitState state = CircuitState.CLOSED;
    private int failures;
    private Instant openedAt = Instant.EPOCH;
    private boolean halfOpenProbeInFlight;
    private long generation;

    CircuitBreaker(int failureThreshold, Duration openDuration) {
        if (failureThreshold <= 0 || openDuration.isNegative() || openDuration.isZero()) {
            throw new IllegalArgumentException("invalid circuit breaker configuration");
        }
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
    }

    synchronized Optional<CircuitPermit> tryAcquire(Instant now) {
        if (state == CircuitState.CLOSED) {
            return Optional.of(new CircuitPermit(generation));
        }
        if (state == CircuitState.OPEN && !now.isBefore(openedAt.plus(openDuration))) {
            state = CircuitState.HALF_OPEN;
            halfOpenProbeInFlight = false;
        }
        if (state == CircuitState.HALF_OPEN && !halfOpenProbeInFlight) {
            halfOpenProbeInFlight = true;
            return Optional.of(new CircuitPermit(generation));
        }
        return Optional.empty();
    }

    synchronized void recordSuccess(CircuitPermit permit) {
        if (permit.generation() != generation || state == CircuitState.OPEN) {
            return;
        }
        failures = 0;
        halfOpenProbeInFlight = false;
        if (state == CircuitState.HALF_OPEN) {
            generation++;
        }
        state = CircuitState.CLOSED;
    }

    synchronized void recordFailure(CircuitPermit permit, Instant now) {
        if (permit.generation() != generation || state == CircuitState.OPEN) {
            return;
        }
        if (state == CircuitState.HALF_OPEN) {
            open(now);
            return;
        }
        failures++;
        if (failures >= failureThreshold) {
            open(now);
        }
    }

    private void open(Instant now) {
        state = CircuitState.OPEN;
        openedAt = now;
        halfOpenProbeInFlight = false;
        generation++;
    }
}
```

### 必测用例

- 连续失败达到阈值后从 CLOSED 进入 OPEN，OPEN 期间请求被快速拒绝。
- 等待期后只允许受控探针进入 HALF_OPEN，成功和失败分别触发正确迁移。
- 并发探针不能无限放行，统计窗口过期后旧失败应被移除。

### 生产化差异

- 示例使用连续失败计数；生产实现还要有最小样本数、滑动窗口失败率和慢调用率。
- 熔断器应按依赖和调用类型隔离，不能让一个下游的失败污染其他调用。
- permit 携带状态代次，避免熔断前的迟到结果错误关闭新一代熔断状态。
- HALF_OPEN 探针必须设置超时并释放占用，否则丢失结果会让恢复探测永久停住。
- 需要配套降级、指标和恢复斜坡；熔断只保护调用方，不修复下游。

# 645 手写熔断器状态机。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
enum CircuitState {
    CLOSED, OPEN, HALF_OPEN
}

final class CircuitBreaker {
    private CircuitState state = CircuitState.CLOSED;
    private int failures;

    synchronized boolean allowRequest() {
        return state != CircuitState.OPEN;
    }

    synchronized void recordSuccess() {
        failures = 0;
        state = CircuitState.CLOSED;
    }

    synchronized void recordFailure() {
        failures++;
        if (failures >= 5) {
            state = CircuitState.OPEN;
        }
    }
}
```

### 必测用例

- 连续失败达到阈值后从 CLOSED 进入 OPEN，OPEN 期间请求被快速拒绝。
- 等待期后只允许受控探针进入 HALF_OPEN，成功和失败分别触发正确迁移。
- 并发探针不能无限放行，统计窗口过期后旧失败应被移除。

### 生产化差异

- 熔断器应按依赖和调用类型隔离，配置基于最小样本、失败率和慢调用率。
- 需要配套降级、指标和恢复斜坡；熔断只保护调用方，不修复下游。

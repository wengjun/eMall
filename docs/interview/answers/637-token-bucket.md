# 637 手写令牌桶限流器。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
final class TokenBucket {
    private final long capacity;
    private final long refillPerSecond;
    private long tokens;
    private long lastRefillNanos;

    TokenBucket(long capacity, long refillPerSecond) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    synchronized boolean tryAcquire(long permits) {
        refill();
        if (permits <= tokens) {
            tokens -= permits;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        long added = elapsed * refillPerSecond / 1_000_000_000L;
        if (added > 0) {
            tokens = Math.min(capacity, tokens + added);
            lastRefillNanos = now;
        }
    }
}
```

### 必测用例

- 初始令牌、连续耗尽、按时间补充和令牌不超过桶容量。
- 使用可控时钟验证小数补充、长时间空闲和时钟回拨边界。
- 多线程同时取令牌时，成功数不能超过当前预算。

### 生产化差异

- 单实例限流要使用单调时钟并导出拒绝率；全局配额需由网关或 Redis Lua 等原子方案协调。
- 还要定义突发容量、公平性、配置热更新和限流组件失效时的策略。

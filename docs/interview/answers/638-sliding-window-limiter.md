# 638 手写滑动窗口限流器。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

final class SlidingWindowLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    SlidingWindowLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    synchronized boolean allow() {
        long now = Instant.now().toEpochMilli();
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMillis) {
            timestamps.removeFirst();
        }
        if (timestamps.size() >= maxRequests) {
            return false;
        }
        timestamps.addLast(now);
        return true;
    }
}
```

### 必测用例

- 窗口内达到上限后拒绝，最早请求过期后立即恢复额度。
- 重点验证恰好位于窗口边界的请求，避免比较符号导致多放或少放。
- 并发请求下通过数不能超过上限，长期运行后旧时间戳应被清理。

### 生产化差异

- 本地队列只适合单实例；分布式限流需要原子更新、时间基准和热点键容量设计。
- 高基数主体应限制状态数量，否则限流器本身会成为内存风险。

# 644 手写重试工具，支持指数退避和 jitter。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

final class Retry {
    static <T> T call(Supplier<T> supplier, int maxAttempts, Duration baseDelay) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException ex) {
                last = ex;
                sleepWithJitter(baseDelay.toMillis() * (1L << Math.min(attempt - 1, 10)));
            }
        }
        throw last;
    }

    private static void sleepWithJitter(long millis) {
        long jitter = ThreadLocalRandom.current().nextLong(Math.max(1, millis / 2));
        try {
            Thread.sleep(millis / 2 + jitter);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 必测用例

- 可重试异常按最大次数执行，成功后立即停止，永久错误一次即返回。
- 验证退避增长、最大间隔和抖动边界，测试中注入 Sleeper 或时钟避免真实等待。
- 调用线程被中断时立即退出并恢复中断标志。

### 生产化差异

- 重试必须受端到端超时和全链路次数预算约束，并仅用于幂等操作。
- 优先使用成熟治理组件，记录尝试次数、最终结果和下游过载，防止多层重试放大。

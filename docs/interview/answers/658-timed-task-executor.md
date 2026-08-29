# 658 手写限时任务执行器。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

static <T> T runWithTimeout(Supplier<T> task, Duration timeout, Executor executor) {
    return CompletableFuture.supplyAsync(task, executor)
            .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .join();
}
```

### 必测用例

- 任务正常完成、抛异常和超时分别返回明确结果。
- 超时后验证取消信号已发出，并用可中断任务确认底层线程实际退出。
- 调用方中断、重复关闭和执行器饱和都应有确定行为。

### 生产化差异

- Future 超时不会自动让不可中断代码停止，任务必须配合中断和下游超时。
- 执行器应由应用生命周期管理，配置有界队列、拒绝策略和超时指标。

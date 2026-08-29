# 659 手写一个可关闭的后台 worker。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.util.concurrent.atomic.AtomicBoolean;

final class CloseableWorker implements AutoCloseable, Runnable {
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            doOneBatch();
        }
    }

    @Override
    public void close() {
        running.set(false);
    }

    private void doOneBatch() {
    }
}
```

### 必测用例

- 任务按约定处理，关闭前已接收任务是排空还是丢弃必须可验证。
- 工作线程阻塞时 `close` 能唤醒并在期限内结束，重复关闭保持幂等。
- 关闭后提交新任务被拒绝，任务异常不会导致线程静默死亡。

### 生产化差异

- 采用显式 RUNNING、STOPPING、TERMINATED 状态并暴露存活、积压和关闭耗时。
- 结合 Spring 生命周期和优雅停机预算，不依赖守护线程自动退出。

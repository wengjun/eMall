# 642 手写简化线程池。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

final class SimpleThreadPool {
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

    SimpleThreadPool(int workers) {
        for (int i = 0; i < workers; i++) {
            Thread thread = new Thread(this::runWorker, "simple-pool-" + i);
            thread.start();
        }
    }

    void submit(Runnable task) {
        tasks.add(task);
    }

    private void runWorker() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                tasks.take().run();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

### 必测用例

- 任务被执行且不重复，队列满时行为符合约定的拒绝策略。
- 单个任务抛异常后工作线程仍能继续处理后续任务。
- 验证优雅关闭、超时强制关闭、重复关闭和关闭后拒绝新任务。

### 生产化差异

- 面试实现用于解释工作线程和任务队列，生产中应使用 `ThreadPoolExecutor`。
- 核心线程、队列、拒绝、线程命名、异常处理和停机期限都必须配置并监控。

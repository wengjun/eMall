# 090 CountDownLatch、CyclicBarrier、Semaphore 分别适合什么场景？

[返回按分类学习面试题](../README.md)

## API 与生命周期速查

| 类型 | 等待/释放 | 需要记住的 Java 语义 |
| --- | --- | --- |
| CountDownLatch | await / countDown | 一次性倒计数，归零后不能复位 |
| CyclicBarrier | await | 固定参与方会合，可复用；中断或超时可能打破屏障 |
| Semaphore | acquire / release | 许可计数，没有锁那样的线程所有权 |

Latch 的 countDown 通常放在工作任务的 finally 中，但“已结束”不等于“成功”，结果仍需另存或用 Future。
Barrier 要处理 `BrokenBarrierException`，不要假设某个参与者失败后其他线程还能正常通过。

## Semaphore 的成对释放

```java
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

final class ConcurrentCalls {
    private final Semaphore permits = new Semaphore(16);

    <T> T execute(Supplier<T> call) {
        if (!permits.tryAcquire()) {
            throw new RejectedExecutionException("concurrency limit reached");
        }
        try {
            return call.get();
        } finally {
            permits.release();
        }
    }
}
```

未获得许可不能 release，否则会把上限越放越大。这个示例只约束同步 call 的整个执行期；
若 call 返回 CompletableFuture，必须在异步完成时释放，而不是返回 Future 时立即释放。
如果改用可中断的 acquire，继续向上抛出 InterruptedException 或恢复中断，不要吞掉它。

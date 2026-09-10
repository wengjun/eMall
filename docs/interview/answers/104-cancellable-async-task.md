# 104 如何设计可取消的 Java 异步任务？

[返回按分类学习面试题](../README.md)

## 区分三个 API 的语义

`Future.get(timeout)` 只限制等待；常见 ExecutorService.submit 返回的 FutureTask 可以通过
`cancel(true)` 请求中断执行线程；CompletableFuture 的 cancel 不保证中断执行中的计算。
不要把这三者统一理解成“超时后自动停止”。

下面片段在拥有 Executor 生命周期的方法中执行，工作任务只做可中断的演示等待：

```java
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

var executor = Executors.newSingleThreadExecutor();
try {
    var future = executor.submit(() -> {
        TimeUnit.SECONDS.sleep(10);
        return "completed";
    });
    try {
        future.get(100, TimeUnit.MILLISECONDS);
    } catch (TimeoutException exception) {
        future.cancel(true);
    }
} finally {
    executor.shutdownNow();
}
```

片段所在方法需要处理或声明 InterruptedException、ExecutionException。
若工作是 CPU 循环，应周期检查中断；若是 IO，要配置对应客户端超时，并确认它如何响应取消。

`shutdownNow` 只是尝试中断，不保证任务立刻退出。Java 17 的 ExecutorService 不能直接放入
try-with-resources。资源清理和中断传播细节看 [103](103-java-interruption.md)。

取消是本地执行控制，不会回滚已提交的数据库更新；此处不重复分布式补偿设计。

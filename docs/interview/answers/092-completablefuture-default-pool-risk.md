# 092 CompletableFuture 的回调到底在哪个线程执行？

[返回按分类学习面试题](../README.md)

## 三种调用不能混为一谈

| 写法 | 执行位置 |
| --- | --- |
| thenApply 等非 Async 回调 | 可能由完成前序阶段的线程或参与完成的调用线程执行 |
| supplyAsync / thenApplyAsync 无 Executor | 通常使用 commonPool |
| Async 方法显式传 Executor | 使用给定执行器，仍需考虑它的拒绝策略 |

Java 17 在公共池并行度不足 2 时，默认异步执行机制可能改为每任务新建线程，
而不是你预想的固定公共线程池。契约见
[CompletableFuture](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletableFuture.html)。

```java
var result = CompletableFuture.supplyAsync(() -> loadOrder(orderId), queryExecutor)
        .thenApplyAsync(this::toView, mappingExecutor);
```

这是局部片段，依赖与 Executor 由调用组件管理。
第一阶段指定了 queryExecutor，不代表后续所有 Async 阶段会自动继承这个池；
若第二阶段不传 mappingExecutor，仍可能回到默认执行器。

非 Async 回调中放慢 IO，可能阻塞正在完成结果的网络线程。
上下文传播需要另行配置，见 [143](143-trace-id-propagation.md)。
本题不重复隔离池和过载架构。

# 098 如何避免 Java 线程池中的任务饥饿和失控？

[返回按分类学习面试题](../README.md)

## 不重复雪崩架构，只看 Executor 的行为

一个容易被忽略的死等：池中的全部工作线程都在等待同一池里尚未执行的子任务。
线程池没有必要出现锁环，也会没有线程继续推进。

```java
var pool = java.util.concurrent.Executors.newFixedThreadPool(1);
var parent = pool.submit(() -> pool.submit(() -> "result").get());
```

这是反例，不要直接运行后无期限等待。唯一工作线程执行 parent 后阻塞在 get，
子任务只能排队。增大池只是推迟饥饿，正确做法是避免同池阻塞等待，或通过 CompletableFuture 组合依赖。

## 另外三个 API 陷阱

- 无界队列下，增大 maximumPoolSize 往往不起作用，因为任务先排队，队列不满就不会按该值扩容。
- CallerRunsPolicy 可能让 HTTP 或事件循环提交线程直接执行耗时任务；shutdown 后它还会直接丢弃任务。
- submit 会把异常保存在 Future 中。不读取结果，也不在统一完成回调中观察异常，就可能“任务失败却没有日志”。

拒绝处理要让调用者知道失败；不要用 DiscardPolicy 让等待 Future 的代码永远没有结果。
具体配置看 [094](094-threadpool-core-parameters.md)，异步组合看 [091](091-completablefuture-async-composition.md)。

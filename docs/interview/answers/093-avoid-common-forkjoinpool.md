# 093 公共 ForkJoinPool 和 parallelStream 有哪些 Java 使用边界？

[返回按分类学习面试题](../README.md)

## 关注隐式共享

普通 parallelStream 通常使用公共 ForkJoinPool，调用线程也可能参与执行。
没有显式 executor 参数并不代表不占用共享工作线程。
不要依赖把 parallelStream 包进自建池这种做法作为跨 JDK 的稳定调度契约。

```java
var pool = java.util.concurrent.ForkJoinPool.commonPool();
System.out.println(pool.getParallelism());
System.out.println(pool.getPoolSize());
System.out.println(pool.getQueuedTaskCount());
```

这些值表示不同状态，不能把 parallelism、当前线程数和排队量当成同一个数。

## 与普通 ThreadPoolExecutor 不同

ForkJoinPool 面向可拆分计算，使用工作窃取；不能按 ThreadPoolExecutor 的队列加拒绝策略模型直接配置它。
部分阻塞可通过 ManagedBlocker 等协作机制补偿，但不能指望任意 JDBC/HTTP 阻塞都被自动识别并无限补偿。

parallelStream 适合无共享副作用的计算；不要直接在 forEach 中写共享 ArrayList。
若需明确控制 IO 并发，使用显式执行器与 CompletableFuture，并处理完成结果，
见 [091](091-completablefuture-async-composition.md)。
公共池不是一概禁用，关键是清楚这次调用会把什么任务放进去。

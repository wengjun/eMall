# 094 ThreadPoolExecutor 参数怎样影响实际执行顺序？

[返回按分类学习面试题](../README.md)

## 先理解 execute 的接收流程

常见执行顺序是：不足 corePoolSize 时创建工作线程；否则先尝试入队；
队列无法接收时再尝试扩到 maximumPoolSize；仍失败或已经关闭时进入拒绝策略。
并发状态变化还会触发内部复查，所以不能简单以某次 size 快照预测下一次提交一定成功。

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

var pool = new ThreadPoolExecutor(
        4, 8, 30, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(32),
        Executors.defaultThreadFactory(),
        new ThreadPoolExecutor.AbortPolicy());
```

数值只用于理解执行路径。没有队列容量边界时，maximumPoolSize 很可能没有你期待的扩容作用。
具体契约见 [Java 17 ThreadPoolExecutor](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html)。

## 其余参数的边界

keepAliveTime 默认主要作用于非核心线程；allowCoreThreadTimeOut(true) 可让核心线程也按配置超时退出。
核心线程一般按需创建，需提前创建时调用 prestartAllCoreThreads。
示例使用默认 ThreadFactory，实际排障可以加业务线程名前缀，但不要在每次请求里重新创建整个池。

execute 接收 Runnable；submit 包装任务并返回 Future，异常观察方式不同。
任务的完成数不等于成功数，失败结果仍需记录。
拒绝策略见 [097](097-rejection-policy.md)，关闭见 [470](470-graceful-shutdown-config.md)。

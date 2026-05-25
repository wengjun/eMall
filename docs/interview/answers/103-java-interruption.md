# 103 Java 中断机制如何正确使用？

[返回按分类学习面试题](../README.md)

## 题目

Java 中断机制如何正确使用？

## 先给面试官的短答案

Java 中断不是强制杀死线程，而是给线程设置中断标志，通知它应该停止或取消当前工作。
正确使用方式是在线程循环中检查中断标志，阻塞方法捕获 `InterruptedException` 后恢复中断状态或退出任务。

不要吞掉中断异常，否则上层无法知道任务已被取消。

## 中断是什么？

调用：

```java
thread.interrupt();
```

会设置目标线程的中断标志。

它不会直接终止线程。

线程是否退出，取决于任务代码是否响应中断。

## 检查中断

长循环任务应检查：

```java
while (!Thread.currentThread().isInterrupted()) {
    doWork();
}
```

这样收到中断后能尽快退出。

## InterruptedException

一些阻塞方法会响应中断并抛出 `InterruptedException`：

- `Thread.sleep()`。
- `Object.wait()`。
- `BlockingQueue.take()`。
- `Thread.join()`。

捕获后通常要做两件事之一：

- 退出任务。
- 恢复中断状态并交给上层处理。

## 恢复中断状态

错误写法：

```java
try {
    queue.take();
} catch (InterruptedException e) {
    log.warn("Interrupted", e);
}
```

这样会吞掉中断。

更好的写法：

```java
try {
    queue.take();
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return;
}
```

恢复中断状态后退出，让上层知道线程已被中断。

## 资源清理

响应中断时要释放资源：

- 锁。
- 文件句柄。
- 数据库连接。
- 临时文件。
- 业务上下文。

通常使用 `finally`。

## 不要使用 Thread.stop

`Thread.stop()` 已废弃，不应该使用。

它会强制终止线程，可能让对象处于不一致状态，破坏锁保护的临界区。

Java 推荐协作式取消，而不是强杀线程。

## 在 eMall 项目中怎么讲？

订单异步补偿任务如果收到关闭信号，应该停止拉取新任务，完成或安全中止当前任务，保存处理进度。

不能简单吞掉 `InterruptedException`，否则服务关闭时线程无法退出，Pod 终止会变慢甚至强杀。

## 深度增强：并发治理图

![Java 并发从线程安全到容量保护](../assets/concurrency-governance.svg)

并发题不能只回答 API 用法。生产系统要同时考虑线程安全、资源隔离、超时、拒绝、幂等和分布式多实例。
单机锁只能保护当前 JVM，不能保护整个集群；线程池满也不是小问题，而是容量和可用性风险。

## 深度增强：Java 17 有界并发示例

```java
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

final class BulkheadGuard {
    private final Semaphore permits;

    BulkheadGuard(int maxConcurrentCalls) {
        this.permits = new Semaphore(maxConcurrentCalls);
    }

    <T> T execute(Supplier<T> supplier) {
        if (!permits.tryAcquire()) {
            throw new IllegalStateException("Bulkhead rejected the call");
        }
        try {
            return supplier.get();
        } finally {
            permits.release();
        }
    }
}
```

这段代码展示了并发控制的生产思路：不是让所有请求无限进入系统，而是在入口保护共享资源。
真实项目还要加超时、指标、降级和按下游隔离。

## 深度增强：生产边界

线程安全不等于系统安全。`ConcurrentHashMap` 只能保护当前进程内的数据结构，
不能替代数据库唯一键、幂等表或分布式一致性。线程池也不能使用无界队列，
否则会把过载转化成内存上涨和 P99 恶化。

## 深度增强：面试高分表达

我会把并发问题分成三层：JMM 和锁保证单机正确性，线程池和隔离保证资源不被拖垮，
幂等和唯一键保证分布式正确性。这样能体现我理解 Java 并发，也理解微服务生产稳定性。

## 专家级完整回答

```text
Java 中断是协作式取消，不是强杀线程。interrupt 只是设置中断标志，可中断阻塞方法会抛出
InterruptedException。正确做法是在循环中检查 isInterrupted，捕获 InterruptedException 后恢复中断状态
并退出或交给上层，同时在 finally 中清理资源。

不要吞掉中断，也不要使用 Thread.stop。生产任务要设计成可取消、可清理、可恢复。
```

## 回答评分点

高分答案应该覆盖：

- 中断不是强制停止。
- 检查 `isInterrupted()`。
- 捕获异常后恢复中断状态。
- 清理资源。
- 不使用 `Thread.stop()`。

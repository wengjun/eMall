# 104 如何设计可取消的异步任务？

[返回按分类学习面试题](../README.md)

## 题目

如何设计可取消的异步任务？

## 先给面试官的短答案

可取消异步任务要采用协作式取消：任务定期检查取消标记或中断状态，阻塞 IO 设置超时，取消时释放资源，
并保证业务幂等和状态可恢复。对 `Future` 可以使用 `cancel(true)`，但任务代码必须响应中断才有效。

取消设计要覆盖线程、IO、业务状态和补偿。

## 取消标记

可以使用取消标记：

```java
class JobContext {
    private final AtomicBoolean cancelled = new AtomicBoolean();

    boolean isCancelled() {
        return cancelled.get();
    }

    void cancel() {
        cancelled.set(true);
    }
}
```

任务循环中检查：

```java
if (context.isCancelled()) {
    return;
}
```

## 响应中断

如果任务提交到线程池，取消时可以：

```java
future.cancel(true);
```

任务内部要响应：

```java
if (Thread.currentThread().isInterrupted()) {
    return;
}
```

阻塞方法捕获 `InterruptedException` 后要恢复中断状态并退出。

## IO 超时

异步任务经常调用外部系统。

必须设置：

- HTTP connect timeout。
- HTTP read timeout。
- DB query timeout。
- Redis command timeout。
- MQ send timeout。

否则任务收到取消信号后，仍可能卡在不可中断 IO 上。

## 分阶段提交

长任务要拆成阶段。

每个阶段完成后记录进度。

取消发生时，任务可以：

- 停止后续阶段。
- 保存当前状态。
- 释放资源。
- 交给补偿任务恢复。

这比一个巨大事务跑到底更稳定。

## 幂等和补偿

取消可能发生在业务操作中间。

必须设计：

- 幂等 key。
- 状态机。
- 操作日志。
- 补偿任务。
- 可重试边界。

否则取消后重试可能造成重复扣款、重复发货或重复扣库存。

## 在 eMall 项目中怎么讲？

订单超时关闭任务可以设计成可取消：

- 扫描待关闭订单。
- 每批处理前检查取消标记。
- 数据库更新使用状态条件和幂等。
- 调用库存释放时设置超时。
- 记录处理进度。
- 服务关闭时停止拉新任务。

这样 Pod 下线不会留下不可控后台任务。

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
可取消异步任务不能只依赖 future.cancel(true)。我会设计协作式取消：任务检查取消标记和中断状态，
阻塞 IO 设置底层超时，捕获 InterruptedException 后恢复中断并退出，finally 释放资源。
业务上要用状态机、幂等 key、操作日志和补偿，保证任务在任意阶段取消后都可恢复。
```

## 回答评分点

高分答案应该覆盖：

- 协作式取消。
- 检查取消标记和中断状态。
- IO 要有超时。
- 资源清理。
- 幂等、状态机和补偿。

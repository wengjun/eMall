# 091 CompletableFuture 如何处理异步编排？

[返回按分类学习面试题](../README.md)

## 题目

`CompletableFuture` 如何处理异步编排？

## 先给面试官的短答案

`CompletableFuture` 用来表达一个未来完成的异步结果，并提供串行、并行、合并、异常处理和超时控制能力。
常见方法包括 `thenApply`、`thenCompose`、`thenCombine`、`allOf`、`exceptionally`、`handle` 和 `orTimeout`。

生产使用时必须指定业务线程池，控制超时、异常和取消，避免把异步编排变成线程池雪崩。

## 基本模型

同步代码是当前线程等待结果。

异步代码是先提交任务，之后通过回调处理结果。

```java
CompletableFuture<Order> future = CompletableFuture.supplyAsync(() -> loadOrder(orderId), executor);
```

`future` 表示未来会得到一个 `Order`。

## 串行转换

`thenApply` 用于结果转换。

```java
CompletableFuture<OrderView> viewFuture = orderFuture.thenApply(this::toView);
```

适合当前步骤不再发起新的异步任务。

## 串行异步依赖

`thenCompose` 用于一个异步任务依赖另一个异步任务的结果。

```java
CompletableFuture<Payment> paymentFuture = orderFuture.thenCompose(order -> queryPayment(order.id()));
```

它会把嵌套的 `CompletableFuture<CompletableFuture<T>>` 展平成 `CompletableFuture<T>`。

## 并行合并

`thenCombine` 用于合并两个独立异步结果。

```java
CompletableFuture<OrderView> result = orderFuture.thenCombine(userFuture, this::merge);
```

适合订单详情同时查订单和用户信息，然后合并。

## 等待多个任务

`allOf` 用于等待多个异步任务完成。

```java
CompletableFuture<Void> all = CompletableFuture.allOf(priceFuture, inventoryFuture, promotionFuture);
```

注意 `allOf` 返回 `CompletableFuture<Void>`，要自己从原 future 中取结果。

## 异常处理

常用方法：

- `exceptionally`：异常时返回兜底值。
- `handle`：同时处理正常结果和异常。
- `whenComplete`：记录日志或指标，不改变结果。

生产代码不能让异步异常静默丢失。

## 超时控制

Java 9 以后可以使用：

```java
future.orTimeout(200, TimeUnit.MILLISECONDS);
future.completeOnTimeout(defaultValue, 200, TimeUnit.MILLISECONDS);
```

超时控制非常重要。

没有超时的异步编排会让请求一直占用资源。

## 生产注意点

必须注意：

- 指定业务线程池。
- 控制线程池队列。
- 设置超时。
- 处理异常。
- 避免阻塞调用 `join()`。
- 避免公共 ForkJoinPool。
- 避免异步任务无限扩散。

异步不是免费并发，背后仍然消耗线程和下游资源。

## 在 eMall 项目中怎么讲？

订单详情页可以并行查询：

- 订单基础信息。
- 物流信息。
- 支付信息。
- 售后状态。

这些查询相互独立，可以用 `CompletableFuture` 并行编排，最后合并结果。

但创建订单链路涉及库存一致性和支付状态，不能为了并行而破坏业务顺序。

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
CompletableFuture 适合表达异步结果并做串行、并行和合并编排。thenApply 做同步转换，
thenCompose 处理异步依赖，thenCombine 合并两个结果，allOf 等待多个任务，exceptionally/handle
处理异常，orTimeout/completeOnTimeout 控制超时。

生产中我一定会指定业务线程池、设置超时和异常处理，并控制队列和下游并发，避免异步任务把线程池和下游打爆。
```

## 回答评分点

高分答案应该覆盖：

- 知道常用编排方法。
- 区分 `thenApply` 和 `thenCompose`。
- 能处理并行合并。
- 必须有异常和超时。
- 必须指定业务线程池。

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

## 深度完善：面向 L6 的回答框架

围绕「CompletableFuture 如何处理异步编排？」，高分答案不能停在概念定义，而要把「线程安全、锁、CAS、线程池、隔离、超时和并发容量」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「并发和线程治理」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `order 创建、inventory 扣减、payment 回调、outbox relay 和异步补偿任务` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：线程池活跃数、队列长度、拒绝数、锁等待、超时率、重复请求数。
- 验证证据：并发单测、压测曲线、线程 dump、拒绝日志、幂等记录和容量评估。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：`CompletableFuture` 如何处理异步编排？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

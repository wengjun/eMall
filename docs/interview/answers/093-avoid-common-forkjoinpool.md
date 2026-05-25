# 093 为什么生产代码不能随意使用公共 ForkJoinPool？

[返回按分类学习面试题](../README.md)

## 题目

为什么生产代码不能随意使用公共 ForkJoinPool？

## 先给面试官的短答案

公共 `ForkJoinPool` 是 JVM 级共享资源，多个框架和业务都可能使用。随意把阻塞 IO、慢任务、
大计算放进去，会造成线程饥饿和业务互相影响。它也不方便按业务设置队列、拒绝、限流和监控。

生产服务应该使用显式、命名、有界、可监控的业务线程池。

## commonPool 的定位

`ForkJoinPool.commonPool()` 是公共共享池。

它常被这些能力隐式使用：

- `CompletableFuture` 默认异步方法。
- parallel stream。
- 某些框架内部任务。

这意味着你不是唯一使用者。

## 风险一：阻塞任务占满线程

ForkJoinPool 适合 fork/join 风格的计算任务。

如果放入阻塞 IO：

- HTTP 调用。
- 数据库查询。
- Redis 调用。
- 文件 IO。

线程会被长时间占住，其他任务无法执行。

## 风险二：缺少业务隔离

公共池没有业务边界。

推荐系统慢可能影响订单查询，报表任务可能影响实时请求。

这类故障很难排查，因为表面看是“线程池慢”，本质是多个业务争抢同一公共资源。

## 风险三：可观测性差

生产线程池需要：

- 线程名。
- active count。
- queue size。
- rejected count。
- task latency。
- 业务标签。

commonPool 很难按业务维度做精细治理。

## parallelStream 的坑

`parallelStream()` 默认也会使用 commonPool。

在 Web 请求中随意使用：

```java
orders.parallelStream().map(this::calculate).toList();
```

可能让请求线程把任务扔进公共池，和其他业务争抢资源。

除非明确评估，否则核心链路应避免随意使用 parallel stream。

## 在 eMall 项目中怎么讲？

营销规则计算如果使用 parallel stream，可能占满 commonPool。

此时订单详情页的 `CompletableFuture` 默认任务也使用 commonPool，就会被营销计算拖慢。

这就是典型的共享池故障扩散。

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
公共 ForkJoinPool 是 JVM 级共享资源，CompletableFuture 默认异步方法和 parallelStream 都可能使用它。
如果把阻塞 IO、慢任务或重计算放进去，会造成线程饥饿和跨业务影响，并且缺少业务级队列、拒绝、
指标和隔离能力。

生产代码应显式使用命名、有界、可监控的业务线程池。核心链路不要随意 parallelStream。
```

## 回答评分点

高分答案应该覆盖：

- commonPool 是共享资源。
- `CompletableFuture` 和 parallel stream 可能使用它。
- 阻塞 IO 会占满线程。
- 缺少隔离和监控。
- 生产使用业务线程池。

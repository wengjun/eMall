# 093 为什么生产代码不能随意使用公共 ForkJoinPool？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

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

![Java 并发从线程安全到容量保护](../../assets/concurrency-governance.svg)

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

## 深度完善：面向 L6 的回答框架

围绕「为什么生产代码不能随意使用公共 ForkJoinPool？」，高分答案不能停在概念定义，而要把「线程安全、锁、CAS、线程池、隔离、超时和并发容量」讲成一条可验证的工程链路。
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

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：为什么生产代码不能随意使用公共 ForkJoinPool？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。


# 092 CompletableFuture 默认线程池有什么风险？

[返回按分类学习面试题](../README.md)

## 题目

`CompletableFuture` 默认线程池有什么风险？

## 先给面试官的短答案

`CompletableFuture.supplyAsync` 如果不指定 Executor，默认使用公共 `ForkJoinPool.commonPool()`。
风险是多个业务共享同一个公共池，阻塞 IO、慢任务或突发流量会互相影响，导致线程饥饿、P99 升高和故障扩散。

生产代码应该为不同业务和下游指定有界线程池。

## 默认行为

示例：

```java
CompletableFuture.supplyAsync(() -> loadOrder(orderId));
```

没有传入 executor 时，会使用默认异步执行器，通常是 `ForkJoinPool.commonPool()`。

这对 demo 方便，对生产服务有风险。

## 风险一：业务互相影响

公共池是共享的。

如果订单查询、推荐计算、营销规则都用默认池，一个业务慢会占用公共线程，影响其他业务。

这违反了隔离原则。

## 风险二：阻塞 IO 不适合 commonPool

`ForkJoinPool` 更适合 CPU 计算和拆分任务。

如果里面执行阻塞 IO，例如 HTTP、数据库、Redis 调用，线程会被长时间占住。

结果是：

- 新任务无法调度。
- 队列堆积。
- P99 升高。
- 故障扩散。

## 风险三：不可控

默认池参数不是按你的业务容量设计的。

你很难针对某个下游设置：

- 最大并发。
- 队列长度。
- 拒绝策略。
- 线程名。
- 指标标签。
- 隔离策略。

没有这些能力，就很难做生产治理。

## 正确方式

生产中显式指定线程池：

```java
CompletableFuture.supplyAsync(() -> loadOrder(orderId), orderQueryExecutor);
```

并且线程池应该：

- 有明确名称。
- 有界队列。
- 有拒绝策略。
- 有指标监控。
- 和业务或下游隔离。
- 设置超时。

## 在 eMall 项目中怎么讲？

订单详情并行查物流、支付和售后时，不应该都丢到默认 commonPool。

更合理的是按下游或业务类型隔离：

- logisticsExecutor。
- paymentQueryExecutor。
- afterSalesExecutor。

支付查询慢不应该拖垮物流和售后查询。

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
CompletableFuture 不指定 Executor 时会使用公共 ForkJoinPool，这在生产中风险很大。
公共池被多个业务共享，阻塞 IO 或慢任务会占满线程，导致无关业务互相影响，而且缺少队列、
拒绝、指标和隔离控制。

我的原则是所有生产异步任务都显式指定有界业务线程池，并按业务或下游隔离，配合超时和监控。
```

## 回答评分点

高分答案应该覆盖：

- 默认使用 commonPool。
- 公共池共享导致互相影响。
- 阻塞 IO 不适合 commonPool。
- 生产要指定 Executor。
- 线程池要有界和可观测。

## 深度完善：面向 L6 的回答框架

围绕「CompletableFuture 默认线程池有什么风险？」，高分答案不能停在概念定义，而要把「线程安全、锁、CAS、线程池、隔离、超时和并发容量」讲成一条可验证的工程链路。
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

本题复习重点：`CompletableFuture` 默认线程池有什么风险？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

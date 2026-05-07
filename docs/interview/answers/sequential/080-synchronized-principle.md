# 080 synchronized 的原理是什么？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

`synchronized` 的原理是什么？

## 先给面试官的短答案

`synchronized` 是 Java 内置锁机制，用对象 monitor 实现互斥和可见性。修饰代码块时，字节码会使用
`monitorenter` 和 `monitorexit`；修饰方法时通过方法访问标志表达同步。进入锁前要获取对象 monitor，
退出锁时释放 monitor，并建立 happens-before 关系，保证同步块内写入对后续获取同一锁的线程可见。

## synchronized 锁的是什么？

`synchronized` 锁的是对象。

示例：

```java
synchronized (lock) {
    update();
}
```

这里锁的是 `lock` 对象。

实例同步方法锁的是当前对象 `this`。

```java
public synchronized void update() {
}
```

静态同步方法锁的是类对象。

```java
public static synchronized void refresh() {
}
```

锁的是 `Class` 对象。

## 字节码层原理

同步代码块会编译成类似：

```text
monitorenter
...
monitorexit
```

进入同步块时获取 monitor，正常退出或异常退出都要释放 monitor。

编译器会保证异常路径也执行 `monitorexit`。

## monitor 是什么？

每个 Java 对象都可以关联一个 monitor。

monitor 可以理解为 JVM 层面的锁结构，维护：

- 当前持锁线程。
- 进入计数。
- 等待队列。
- 阻塞线程。

当线程获取不到 monitor 时，会进入阻塞等待。

## 可重入性

`synchronized` 是可重入锁。

同一个线程已经持有某个对象锁时，可以再次进入同一把锁保护的代码。

示例：

```java
synchronized void outer() {
    inner();
}

synchronized void inner() {
}
```

同一线程调用 `outer` 后再进入 `inner` 不会死锁。

monitor 会记录重入次数，退出时逐层释放。

## 可见性

`synchronized` 不只是互斥，也保证可见性。

规则是：

- 释放锁前，会把工作内存中的修改刷新出去。
- 获取同一把锁后，能看到之前释放锁线程的写入。

这对应 Java 内存模型中的 happens-before：

```text
unlock happens-before subsequent lock on the same monitor
```

## 锁升级

HotSpot 曾经有偏向锁、轻量级锁、重量级锁等优化。

Java 17 中偏向锁已经被废弃并默认不可用，但理解锁优化仍有价值。

锁竞争低时，JVM 会尽量用较轻量方式处理；竞争激烈时，会膨胀为更重的 monitor。

面试时不要把旧版本偏向锁细节当成 Java 17 当前默认行为。

## synchronized 的优点

优点：

- 语法简单。
- 自动释放锁。
- 异常时不容易忘记解锁。
- JVM 深度优化。
- 语义清晰。

适合临界区短、竞争不高、逻辑简单的场景。

## synchronized 的风险

风险：

- 锁粒度过大。
- 锁内做 IO。
- 多锁嵌套导致死锁。
- 热点锁导致 P99 升高。
- 无法像 `ReentrantLock` 那样灵活尝试加锁或中断等待。

生产中锁内代码要尽量短。

## 在 eMall 项目中怎么讲？

如果库存扣减用全局 `synchronized`，所有商品都会串行，吞吐很差。

更合理的是按商品 ID 分段、使用数据库条件更新、Redis 原子操作或库存桶。

`synchronized` 可以用于保护本地小范围状态，但不能粗暴保护整个核心交易流程。

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
synchronized 基于对象 monitor 实现。同步代码块在字节码中使用 monitorenter/monitorexit，
同步方法通过方法访问标志实现。它既提供互斥，也提供可见性：对同一 monitor 的 unlock
happens-before 后续 lock。它还是可重入锁，同一线程可以重复进入同一把锁。

工程上我会控制锁粒度，避免锁内 IO 和多锁嵌套。对库存、秒杀这类热点场景，不会用全局
synchronized，而会按 key 拆分、使用数据库原子条件更新或库存桶。
```

## 回答评分点

高分答案应该覆盖：

- 锁的是对象 monitor。
- 同步代码块对应 `monitorenter`/`monitorexit`。
- 同步方法锁 `this` 或 `Class` 对象。
- `synchronized` 可重入。
- 提供互斥和可见性。
- 能联系锁粒度和热点业务。

## 深度完善：面向 L6 的回答框架

围绕「synchronized 的原理是什么？」，高分答案不能停在概念定义，而要把「线程安全、锁、CAS、线程池、隔离、超时和并发容量」讲成一条可验证的工程链路。
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

本题复习重点：`synchronized` 的原理是什么？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

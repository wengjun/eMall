# 082 ReentrantLock 和 synchronized 怎么选？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

`ReentrantLock` 和 `synchronized` 怎么选？

## 先给面试官的短答案

简单同步优先用 `synchronized`，因为语法简单、自动释放、JVM 优化充分。需要可中断获取锁、超时尝试、
公平锁、多条件队列或更灵活控制时，选择 `ReentrantLock`。

选择标准不是哪个更高级，而是业务是否需要 `ReentrantLock` 的额外能力。

## synchronized 的特点

优点：

- 语法简单。
- 自动释放锁。
- 异常时不容易忘记解锁。
- JVM 内置优化。
- 可读性好。

缺点：

- 不能尝试加锁后立即返回。
- 不能设置加锁超时。
- 不能响应中断等待。
- 条件队列能力较弱。

适合简单临界区。

## ReentrantLock 的特点

`ReentrantLock` 是显式锁。

优点：

- `tryLock()`。
- `tryLock(timeout)`。
- `lockInterruptibly()`。
- 可选择公平锁。
- 支持多个 `Condition`。
- 可查询锁状态。

缺点：

- 必须手动释放锁。
- 忘记 `unlock()` 会导致严重故障。
- 代码更复杂。

正确写法：

```java
lock.lock();
try {
    update();
} finally {
    lock.unlock();
}
```

## 什么场景用 synchronized？

适合：

- 临界区很短。
- 锁逻辑简单。
- 不需要超时。
- 不需要中断等待。
- 不需要多个条件队列。

例如保护本地计数器、小缓存元数据、简单状态切换。

## 什么场景用 ReentrantLock？

适合：

- 获取不到锁时要快速失败。
- 等待锁需要超时。
- 等待锁时要支持中断。
- 需要公平锁。
- 需要多个条件队列。
- 需要更复杂同步控制。

例如库存热点保护中，请求等待锁超过 20 ms 就降级或返回重试提示。

## 公平锁选择

`ReentrantLock` 可以创建公平锁：

```java
new ReentrantLock(true)
```

公平锁减少插队，但吞吐通常更低。

默认非公平锁吞吐更好，适合大多数高并发服务。

只有在强公平要求或饥饿风险明显时才考虑公平锁。

## Condition 的价值

`Condition` 可以创建多个等待队列。

相比 `Object.wait/notify`，它更适合复杂同步场景。

例如一个阻塞队列可以分别有：

- notEmpty 条件。
- notFull 条件。

生产业务通常优先使用成熟并发工具，而不是自己写复杂条件同步。

## 在 eMall 项目中怎么讲？

eMall 中简单本地状态保护可以用 `synchronized`。

如果秒杀热点商品需要获取锁失败后快速降级，可以用 `ReentrantLock.tryLock(timeout)`，避免请求无限等待。

如果是跨实例库存一致性，单机锁都不够，要使用数据库条件更新、Redis 原子操作或消息串行化。

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
简单互斥优先 synchronized，因为它语义简单、自动释放、JVM 优化充分。只有当我需要 tryLock、
超时获取、可中断等待、公平锁或多个 Condition 时，才选择 ReentrantLock。ReentrantLock 必须在
finally 中 unlock，否则会造成严重故障。

锁选择还要看边界：单机锁只能保护当前 JVM，不能解决多实例并发。库存这类跨实例一致性问题，
通常需要数据库条件更新、Redis 原子操作或消息化设计。
```

## 回答评分点

高分答案应该覆盖：

- 简单场景优先 `synchronized`。
- `ReentrantLock` 支持超时、中断、公平和 Condition。
- 显式锁必须 finally 解锁。
- 公平锁吞吐通常更低。
- 单机锁不能解决分布式并发。

## 二次深度补强

题目：`ReentrantLock` 和 `synchronized` 怎么选？

二次补强标记：已完成

### 面试官真正想确认的能力

并发题要同时回答正确性、吞吐、隔离、超时、取消和故障恢复。
围绕这道题，要进一步把概念、项目实现、线上风险和验证闭环连起来。

### 深度和广度补充

- 先定义共享资源和临界区，明确并发冲突在哪里发生。
- 再选择工具：锁、CAS、队列、线程池、信号量、幂等或分布式锁。
- 随后补齐失败路径：超时、重试、取消、锁过期、线程池耗尽和降级。
- 最后用压测和可观测指标证明方案能承受峰值流量。

### 图片讲解

![二次补强图解](../../assets/concurrency-governance.svg)

- 图中展示入口限流、线程池隔离、锁保护和降级之间的关系。
- 读图时要关注请求什么时候被拒绝，什么时候排队，什么时候快速失败。
- 高分回答要能说明保护的是系统稳定性，而不是单次请求成功率。

### Java17 有界异步执行示例

```java
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

final class BoundedAsyncExecutor implements AutoCloseable {
    private final Semaphore permits = new Semaphore(128);
    private final ExecutorService pool = Executors.newFixedThreadPool(32);

    <T> CompletableFuture<T> submit(Callable<T> task) {
        if (!permits.tryAcquire()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Too many requests."));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                permits.release();
            }
        }, pool);
    }

    @Override
    public void close() {
        pool.shutdown();
    }
}
```

### 高分表达要点

- 不要只回答定义，要说明为什么这样设计、在什么条件下失效、如何监控和回滚。
- 把答案和当前电商项目联系起来，例如订单、库存、支付、履约、搜索、风控或发布链路。
- 主动给出边界条件和反例，能让面试官看到你具备生产系统判断力。

## 逐题专项补强

逐题专项补强标记：已完成

### 本题专项切入

- 本题要围绕「`ReentrantLock` 和 `synchronized` 怎么选？」展开，不要只复述分类模板。
- 先确定共享资源、并发冲突点、超时策略和隔离边界。
- 再说明高峰流量下如何限流、排队、快速失败和恢复。

### 专项图解说明

![逐题专项图解](../../assets/concurrency-governance.svg)

- 这张图用于把「`ReentrantLock` 和 `synchronized` 怎么选？」放回生产链路中理解，重点看入口、状态、数据和恢复闭环。
- 面试时可以先按图说明主路径，再补失败路径、监控指标和回滚手段。

### 贴合本题的实现示例

```java
public record ArchitectureDecision(String goal, String option, String risk) {

    String explain() {
        return goal + " -> choose " + option + ", risk=" + risk;
    }
}
```

### 进一步追问时的回答边界

- 如果面试官继续追问，要主动说明这个实现是核心模型，不等于完整生产组件。
- 生产级落地还需要接入鉴权、幂等、限流、熔断、监控、告警、灰度和数据修复。
- 回答时把复杂度、失败场景、验证方式和 eMall 项目中的落地位置一起说清楚。

## 面试实战补强

面试实战补强标记：已完成

### 面试追问路线

- 这个方案在 100W 最高并发下如何避免线程池、队列或锁成为雪崩点？
- 如果锁过期、任务超时、线程被中断或请求重试，会不会破坏正确性？
- 你如何设计拒绝策略、超时策略、降级策略和监控指标？

### eMall 项目落点

- 可以落到模块：gateway、traffic、flash-sale、inventory。
- 回答「`ReentrantLock` 和 `synchronized` 怎么选？」时，要从这些模块里选一个主链路做例子。
- 讲清入口、状态变化、数据写入、异步事件、失败补偿和观测指标。

### 生产验证指标

- 线程池活跃数
- 队列长度
- 拒绝次数
- 锁等待时间

### 低分陷阱

- 只背定义，不说明业务场景和失败场景。
- 只讲正常路径，不讲超时、重试、回滚、补偿和监控。
- 只给方案，不给验证指标和取舍边界。

### 30 秒高分收束

这道题我会用 并发、线程池、分布式锁 的视角回答。
先给结论，再给项目例子，然后补失败场景、验证指标和取舍边界。
这样能让面试官看到我不是只会背知识点，而是能把知识点落到生产系统。

## 架构取舍与反驳补强

架构取舍补强标记：已完成

### 先给立场

- 回答「`ReentrantLock` 和 `synchronized` 怎么选？」时，不能只给单一方案，要先说明约束、目标和失败边界。
- 高分回答要让面试官看到你能在正确性、可用性、成本、复杂度和团队能力之间做判断。

### 可选方案对比

- 简单方案：上线快、成本低，但容量和故障边界有限。
- 平台化方案：复用强、治理强，但建设成本和组织协调更高。
- 外部托管方案：交付快，但可控性、成本和供应商风险需要评估。

### 反驳和防守

- 如果面试官问为什么不直接上最复杂方案，可以回答：复杂方案只有在规模和风险证明必要时才值得引入。
- 如果面试官问为什么不用最简单方案，可以回答：简单方案可以做第一期，但必须提前设计观测和迁移边界。
- 我的判断原则是：如果约束不明确，先补齐规模、延迟、可用性、一致性、成本和团队能力，再做选择。

### 决策证据

- 业务指标
- 稳定性指标
- 成本指标
- 灰度和回滚记录

### 一句话总结

我会先用简单可靠的方案解决当前确定性问题，同时保留观测、灰度和迁移能力。
当指标证明瓶颈存在，再演进到更复杂的架构，而不是为了显得高级提前复杂化。

## 生产落地验收补强

生产验收补强标记：已完成

### 上线前检查

- 针对「`ReentrantLock` 和 `synchronized` 怎么选？」，先确认它影响的是正确性、稳定性、性能、安全还是成本。
- 确认需求边界、容量目标、失败场景、回滚路径和责任人。
- 上线前至少完成灰度计划、监控看板、告警规则和复盘模板。

### 灰度和回滚

- 先在测试环境和影子流量中验证，再做 1%、5%、25%、50%、100% 分阶段灰度。
- 每个阶段都设置自动暂停条件和人工回滚负责人。
- 回滚不是只回代码，还要确认配置、数据、缓存、消息和任务状态能一起回到安全状态。

### 监控和验收证据

- 测试报告
- 灰度看板
- 告警规则
- 回滚记录

### 面试表达

我不会只说方案能实现，还会说明上线前怎么验收、上线中怎么看指标、出问题怎么回滚。
这能证明我关注的是长期稳定运行，而不是只完成一次功能开发。

## 规模化与成本治理补强

规模成本补强标记：已完成

### 规模化视角

- 回答「`ReentrantLock` 和 `synchronized` 怎么选？」时，要主动放到 10 亿用户、1 亿 DAU、100W 峰值并发的背景下思考。
- 按并发请求、阻塞比例、下游耗时和队列长度计算线程池容量。
- 锁和队列都要设置上限，避免把流量变成不可控堆积。

### 成本治理

- 用单位成本看问题，例如单请求成本、单订单成本、单消息成本和单 GB 存储成本。
- 先优化浪费最高的环节，而不是平均用力。

### 自动化和 owner

- 为关键指标建立看板、告警、owner 和 Runbook。
- 把经验沉淀成自动化检查、流水线门禁或平台能力。

### 面试表达

我会补一句：方案能跑只是第一步，大规模下还要回答容量怎么估、成本怎么控、故障谁负责。
这能体现我不是只会实现单点功能，而是能长期运营一个高并发业务系统。


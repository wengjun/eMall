# 088 ABA 问题是什么，如何解决？

[返回按分类学习面试题](../README.md)

## 题目

ABA 问题是什么，如何解决？

## 先给面试官的短答案

ABA 问题是 CAS 中当前值从 A 变成 B 又变回 A，CAS 只比较值，发现仍然是 A 就认为没有变化，
但实际上中间发生过修改。解决方式是加版本号或时间戳，例如使用 `AtomicStampedReference`，
或者让状态变化不可逆，避免单纯比较值。

## ABA 示例

线程 1 读取值 A。

线程 2 把 A 改成 B。

线程 2 又把 B 改回 A。

线程 1 执行 CAS：

```text
expected = A
current = A
```

CAS 成功。

但线程 1 不知道中间发生过 A -> B -> A。

## 为什么有风险？

如果业务只关心当前值，ABA 可能没问题。

但如果业务关心变化过程，ABA 就危险。

典型风险：

- 无锁栈节点被移除又放回。
- 资源被释放又复用。
- 状态曾经变化过但被忽略。
- 引用相同但对象语义已经变了。

ABA 本质是“值相同不代表状态没变”。

## 版本号解决

加版本号后，比较的不只是值，还有版本。

变化过程：

```text
A(1) -> B(2) -> A(3)
```

虽然值又是 A，但版本从 1 变成 3，CAS 会失败。

Java 提供：

```java
AtomicStampedReference<T>
```

它可以同时比较引用和 stamp。

## 时间戳或序列号

也可以使用时间戳、递增序列号或状态版本。

例如库存记录：

```text
stock_quantity + version
```

更新时带上版本条件：

```sql
update stock
set quantity = ?, version = version + 1
where sku_id = ? and version = ?
```

这和乐观锁思想一致。

## 让状态不可逆

有些状态可以设计成单向流转，避免 ABA。

例如订单状态：

```text
CREATED -> PAID -> SHIPPED -> COMPLETED
```

正常情况下不允许随意回到旧状态。

状态机设计能减少 ABA 类问题。

## ABA 一定要解决吗？

不一定。

如果业务只关心当前数值，不关心中间变化，ABA 可能无害。

例如简单计数器在某些场景下只要最终值正确即可。

但无锁数据结构、资源生命周期和业务状态变更通常需要考虑 ABA。

## 在 eMall 项目中怎么讲？

库存和订单状态通常不能只比较当前值。

例如订单从 `CREATED` 变成 `CANCELED` 又被错误改回 `CREATED`，只看状态值会掩盖中间变化。

更合理的是使用版本号、状态机和操作日志，确保每次状态变更可追踪、可校验、可幂等。

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
ABA 是 CAS 中值从 A 变 B 又变回 A，CAS 只看到当前值仍是 A，因此误以为没有变化。
如果业务关心状态变化过程，这会导致错误。解决方式是给值加版本号、stamp、时间戳或序列号，
例如 AtomicStampedReference，或者在业务上设计单向状态机和乐观锁版本字段。

是否必须解决取决于业务语义。只关心当前值时 ABA 可能无害，资源生命周期和订单状态通常必须防范。
```

## 回答评分点

高分答案应该覆盖：

- ABA 是 A 变 B 又变 A。
- CAS 只比较当前值会误判。
- 版本号或 stamp 能解决。
- 乐观锁版本字段是业务常见方案。
- 不是所有场景都必须处理 ABA。

# 089 AtomicInteger 和 LongAdder 如何取舍？

[返回按分类学习面试题](../README.md)

## 核心结论

`AtomicInteger` 基于单个变量 CAS，适合低竞争、需要立即获取精确值或需要 CAS 条件更新的场景。
`LongAdder` 通过分散热点到多个 cell 降低竞争，适合高并发计数、指标累加，但读取汇总值不是强一致瞬时值。

简单说：精确状态更新用 Atomic，高并发统计计数用 LongAdder。

## AtomicInteger 特点

`AtomicInteger` 维护一个原子 int 值。

常见操作：

```java
incrementAndGet()
compareAndSet(expected, update)
```

优点：

- 语义简单。
- 读取值精确。
- 支持 CAS 条件更新。
- 适合状态转换。

缺点：

- 高竞争下多个线程争抢同一个变量。
- CAS 失败重试会增加 CPU 消耗。

## LongAdder 特点

`LongAdder` 会在竞争激烈时把计数分散到多个 cell。

不同线程可能更新不同 cell，减少对单点的争抢。

读取时把 base 和 cells 汇总。

优点：

- 高并发累加吞吐高。
- 降低 CAS 热点竞争。

缺点：

- 不支持条件 CAS。
- `sum()` 不是严格线性一致快照。
- 内存占用更高。

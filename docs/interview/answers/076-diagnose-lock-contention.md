# 076 如何定位锁竞争？

[返回按分类学习面试题](../README.md)

## 核心结论

定位锁竞争要看线程状态、阻塞栈和等待时间。常用工具是 `jstack`、JFR、APM 和线程池指标。
如果大量线程处于 `BLOCKED`，或 JFR 中 monitor blocked 时间很高，并且调用栈集中在同一段代码，
就说明存在锁竞争热点。

修复方向是缩小锁粒度、减少锁内逻辑、拆分热点资源、使用无锁结构或异步化。

## 使用 jstack

`jstack` 可以看到线程状态和阻塞位置。

如果很多线程类似：

```text
java.lang.Thread.State: BLOCKED
    at com.example.mall.inventory.InventoryService.reserve(...)
```

说明它们在等待 synchronized monitor。

要关注：

- 是否大量线程卡在同一行。
- 等待的是同一个 monitor。
- 持锁线程在做什么。
- 锁内是否有数据库、HTTP 或复杂计算。

## 使用 JFR

JFR 对锁竞争非常有用。

可以看：

- Java Monitor Blocked。
- Thread Park。
- Lock Instances。
- 阻塞时间。
- 阻塞调用栈。

JFR 比单次 `jstack` 更适合看一段时间内的锁等待分布。

## ReentrantLock 和 park

如果使用 `ReentrantLock`，线程可能表现为 `WAITING` 或 `TIMED_WAITING`，栈中出现 `Unsafe.park`。

这不一定说明线程空闲，可能是在等待锁或条件队列。

要结合：

- 线程名。
- 调用栈。
- JFR park 事件。
- 锁对象和业务代码。

## 修复方向

常见修复：

- 缩小 synchronized 范围。
- 锁内不做 IO。
- 拆分全局锁。
- 按 key 分段锁。
- 使用 CAS 或并发集合。
- 热点库存拆桶。
- 使用队列串行化单个热点。
- 数据库层减少事务范围。

选择哪种方案取决于一致性要求和业务热点形态。

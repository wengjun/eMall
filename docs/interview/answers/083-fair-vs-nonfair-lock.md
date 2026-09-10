# 083 公平锁和非公平锁有什么区别？

[返回按分类学习面试题](../README.md)

## 对应 Java API

`new ReentrantLock()` 默认非公平；`new ReentrantLock(true)` 在竞争时倾向于把锁授予等待最久的线程。
公平性约束的是锁获取，不保证 OS 调度公平，也不意味着任意观测时刻严格按到达顺序运行。

```java
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

var lock = new ReentrantLock(true);
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try {
        System.out.println("acquired");
    } finally {
        lock.unlock();
    }
}
```

容易忽略的区别：不带超时的 `tryLock()` 即使在公平锁上也允许插队；
带超时的版本会遵守公平策略，并响应中断。
这是 [Java 17 ReentrantLock](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html)
明确区分的行为。

不再展开通用调度原理。需要记住公平模式可能降低吞吐，只有明确关注等待时间分布时再评估，
不要为了“更公平”给所有 Java 锁统一打开它。

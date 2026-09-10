# 030 `ConcurrentHashMap` 为什么比 `Hashtable` 更适合并发场景？

[返回按分类学习面试题](../README.md)

## 重点是原子操作，不是换个类名

Hashtable 的单个方法主要通过整表对象锁同步。Java 17 ConcurrentHashMap 采用更细粒度的同步，
读取通常不需要获取更新所用的锁；不要再按 Java 7 的 Segment 数组讲它的实现。

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

var counts = new ConcurrentHashMap<String, LongAdder>();
counts.computeIfAbsent("order.created", key -> new LongAdder()).increment();
```

`computeIfAbsent` 将“检查并安装值”作为原子操作。这里 value 本身也是并发安全的；
换成 ArrayList 后，后续 add 不会因外层 Map 安全而变安全。

## 四个必须知道的边界

- `get` 再 `put` 是两次操作，计数累加应使用 `merge`、`compute` 或原子值。
- 映射函数必须短小，不能在内部递归更新本 Map；慢 HTTP/SQL 会拖住相关更新。
- null key/value 均不允许，`get == null` 才能清楚地表示当前未找到。
- 迭代弱一致，不会像普通 HashMap 那样依赖 fail-fast；但也不是整个 Map 的原子快照。

`size()`、遍历结果与多个 key 的联合状态不构成事务，不能用于严格的并发准入判断。

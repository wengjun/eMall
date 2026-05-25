# 030 `ConcurrentHashMap` 为什么比 `Hashtable` 更适合并发场景？

[返回按分类学习面试题](../README.md)

## 题目

`ConcurrentHashMap` 为什么比 `Hashtable` 更适合并发场景？

## 先给面试官的短答案

`Hashtable` 通过 synchronized 锁住大部分方法，锁粒度粗，并发性能差。
`ConcurrentHashMap` 针对并发访问设计，读操作通常不阻塞，写操作也尽量局部化，
并提供 `computeIfAbsent`、`putIfAbsent` 等原子复合操作。

所以并发场景下，`ConcurrentHashMap` 通常比 `Hashtable` 更合适。

## 从零基础理解

多个线程同时访问 Map 时，普通 `HashMap` 不安全。
早期 Java 提供 `Hashtable`，它通过给方法加锁保证线程安全：

```java
public synchronized V get(Object key) {
}
```

问题是锁太粗。一个线程写入时，其他线程读写也容易被阻塞。

`ConcurrentHashMap` 的目标是提高并发度，让不同 key 的操作尽量不要互相阻塞。

## ConcurrentHashMap 的优势

### 并发性能更好

读操作通常不需要锁住整个 Map。写操作也不是锁整张表。

### 提供原子复合操作

并发代码中，先 get 再 put 不是原子的：

```java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

两个线程可能同时进入。

更好的写法：

```java
map.putIfAbsent(key, value);
```

或：

```java
RateLimiter limiter = limiters.computeIfAbsent(key, ignored -> new RateLimiter());
```

### 迭代弱一致

遍历时允许并发修改，不会像普通集合那样轻易抛 `ConcurrentModificationException`。
但它也不保证遍历看到的是某一瞬间的完整快照。

### 不允许 null

`ConcurrentHashMap` 不允许 null key 和 null value，避免并发场景下无法区分不存在和 value 为 null。

## Hashtable 的问题

- 老旧。
- 锁粒度粗。
- API 设计过时。
- 并发度低。
- 现代项目基本不推荐新代码使用。

## 需要注意的坑

### Map 线程安全不代表 value 线程安全

```java
ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();
```

Map 结构安全，但 `List` 不是线程安全的。

### 多步骤业务逻辑仍要原子化

如果逻辑是：

```text
读取库存 -> 判断 -> 修改库存
```

不能只靠 `ConcurrentHashMap`。多实例部署下还要依赖数据库条件更新、分布式锁或幂等。

### 本地并发容器不能解决分布式并发

`ConcurrentHashMap` 只在当前 JVM 内有效。多个 Pod 各有一份 Map，不能用它做全局幂等。

## 在 eMall 项目中怎么讲？

适合用 `ConcurrentHashMap` 的地方：

- 本地内存限流器缓存。
- 本地测试用 InMemoryRepository。
- 本地任务状态。
- 非关键路径的进程内缓存。

不适合：

- 生产全局幂等。
- 订单去重最终兜底。
- 库存防超卖。
- 支付回调去重最终兜底。

这些必须依赖数据库唯一约束、持久化记录或分布式协调。

## 专家级完整回答

```text
Hashtable 通过 synchronized 锁住方法，锁粒度粗，并发性能差。
ConcurrentHashMap 是为并发访问设计的，读操作通常不阻塞，写操作尽量局部化，
并提供 putIfAbsent、computeIfAbsent 这类原子复合操作。

但 ConcurrentHashMap 只保证当前 JVM 内 Map 结构的线程安全，不代表 value 线程安全，
也不能解决多实例分布式并发。在电商系统里，它适合本地缓存和本地限流器，
但订单幂等、库存防超卖、支付回调去重必须由数据库约束或持久化幂等记录兜底。
```

## 回答评分点

高分答案应该覆盖：

- Hashtable 锁粒度粗。
- ConcurrentHashMap 并发度更高。
- 能说出 putIfAbsent、computeIfAbsent。
- 能指出 value 不一定线程安全。
- 能指出它不能解决分布式并发。

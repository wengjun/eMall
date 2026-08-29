# 084 volatile 解决什么问题，不能解决什么问题？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`volatile` 解决可见性和一定的有序性问题，保证一个线程写入 volatile 变量后，其他线程能及时看到，
并通过内存屏障限制相关指令重排序。但它不能保证复合操作的原子性，例如 `count++` 仍然不是线程安全的。

所以 `volatile` 适合状态标记、开关、配置引用发布，不适合并发计数和复杂状态更新。

## 可见性问题

多线程中，一个线程修改变量，另一个线程不一定立刻看到。

原因是线程可能使用工作内存、CPU 缓存和编译优化。

`volatile` 写入会让修改对其他线程可见。

示例：

```java
private volatile boolean running = true;
```

一个线程修改：

```java
running = false;
```

另一个线程循环读取时能更可靠地看到变化。

## 有序性问题

编译器和 CPU 可能为了性能重排序指令。

`volatile` 会通过内存屏障限制重排序。

典型语义：

- volatile 写之前的普通写不能重排到 volatile 写之后。
- volatile 读之后的普通读写不能重排到 volatile 读之前。

这对安全发布配置对象很重要。

## 不保证复合操作原子性

`count++` 看起来是一行，实际包含：

- 读取 count。
- 加 1。
- 写回 count。

即使 count 是 volatile，多个线程仍然可能同时读到相同旧值，导致丢失更新。

错误示例：

```java
private volatile int count;

void increment() {
    count++;
}
```

并发计数应该用 `AtomicInteger`、`LongAdder` 或锁。

## 适合场景

适合：

- 停止标记。
- 开关变量。
- 配置引用。
- 单写多读状态。
- 双重检查锁中的实例引用。

示例：

```java
private volatile PricingConfig currentConfig;
```

配置整体不可变，替换引用时用 volatile 保证可见。

## 不适合场景

不适合：

- 并发累加。
- 多字段一致更新。
- 读改写复合逻辑。
- 需要互斥的临界区。
- 复杂状态机。

这些场景需要锁、原子类或更高层并发结构。

## volatile 和 synchronized 的区别

`volatile`：

- 保证可见性。
- 保证一定有序性。
- 不保证复合操作原子性。
- 不提供互斥。

`synchronized`：

- 保证互斥。
- 保证可见性。
- 可保护复杂临界区。
- 成本相对更高。

## 在 eMall 项目中怎么讲？

营销配置热更新可以用 volatile 保存不可变配置引用。

```java
private volatile PromotionRules activeRules;
```

更新线程整体替换 `activeRules`，请求线程读取当前引用。

但库存扣减不能靠 volatile，因为库存扣减是读改写，需要数据库条件更新、Redis 原子操作或锁。

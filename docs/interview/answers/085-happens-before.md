# 085 happens-before 规则是什么？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

happens-before 是 Java 内存模型中判断可见性和有序性的规则。如果操作 A happens-before 操作 B，
那么 A 的结果对 B 可见，并且 A 的执行顺序在内存语义上先于 B。它不是简单的时间先后，而是内存可见性保证。

常见规则包括程序顺序、锁释放先于后续加锁、volatile 写先于后续读、线程 start 和 join 规则。

## 为什么需要 happens-before？

多线程中，实际执行顺序、CPU 缓存、编译器优化和指令重排序会让代码表现变复杂。

happens-before 提供了一套规则，让程序员判断：

- 一个线程写入是否对另一个线程可见。
- 哪些操作不能被重排序破坏。
- 什么时候读到的数据是安全的。

它是理解 Java 并发的核心概念。

## 程序顺序规则

同一个线程内，前面的操作 happens-before 后面的操作。

示例：

```java
int a = 1;
int b = a + 1;
```

在同一线程内，`a = 1` 对后面的 `b = a + 1` 可见。

注意这是线程内规则，不代表其他线程一定看到。

## 锁规则

对同一把锁：

```text
unlock happens-before subsequent lock
```

一个线程释放锁前的写入，对后续获取同一把锁的线程可见。

这就是 `synchronized` 能保证可见性的原因。

## volatile 规则

对同一个 volatile 变量：

```text
volatile write happens-before subsequent volatile read
```

一个线程写 volatile 变量，另一个线程后续读到这个 volatile 变量时，能看到写线程在写 volatile 前的相关写入。

这让 volatile 能用于状态标记和配置发布。

## 线程 start 规则

调用线程的 `Thread.start()` happens-before 新线程中的任何操作。

示例：

```java
worker.setConfig(config);
thread.start();
```

新线程能看到 start 前已经设置好的状态。

## 线程 join 规则

线程中的所有操作 happens-before 其他线程从 `join()` 成功返回。

这意味着一个线程结束后，join 它的线程能看到它的执行结果。

## 传递性

happens-before 具有传递性。

如果：

```text
A happens-before B
B happens-before C
```

那么：

```text
A happens-before C
```

传递性让复杂并发程序可以通过多个规则组合推导可见性。

## 电商系统实践

配置中心推送新优惠规则时，可以构造不可变 `PromotionRules`，然后赋值给 volatile 引用。

构造对象时的写入 happens-before volatile 写，后续请求线程 volatile 读后就能看到完整配置。

如果没有这些规则，请求线程可能看到未正确发布的对象。

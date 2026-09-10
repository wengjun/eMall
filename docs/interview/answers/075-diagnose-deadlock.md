# 075 如何定位死锁？

[返回按分类学习面试题](../README.md)

## 核心结论

定位死锁首先看线程 dump，`jstack` 通常能识别 Java monitor 死锁，并输出 involved threads。
然后分析每个线程持有什么锁、等待什么锁、锁对应哪段业务代码。修复方向是固定加锁顺序、减少锁嵌套、
缩小锁粒度、避免锁内调用外部系统，必要时使用带超时的锁。

## 简单示例

```java
synchronized (lockA) {
    synchronized (lockB) {
        updateAThenB();
    }
}

synchronized (lockB) {
    synchronized (lockA) {
        updateBThenA();
    }
}
```

两个线程如果分别持有 `lockA` 和 `lockB`，再等待对方锁，就会死锁。

## 使用 jstack 定位

执行：

```powershell
jstack <pid>
```

如果是典型 Java monitor 死锁，输出可能包含：

```text
Found one Java-level deadlock
```

然后会列出：

- 哪些线程参与死锁。
- 每个线程持有的锁。
- 每个线程等待的锁。
- 对应的 Java 调用栈。

这是最直接的定位方式。

## ReentrantLock 死锁

如果使用 `ReentrantLock`，也可能发生死锁。

某些工具对 ownable synchronizer 也能提供信息，但排查时仍要看：

- 哪个线程持有锁。
- 哪个线程等待锁。
- 等待发生在哪一行。
- 加锁顺序是否相反。

使用 `tryLock(timeout)` 可以避免无限等待，但业务必须处理获取失败的情况。

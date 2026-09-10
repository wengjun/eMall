# 080 synchronized 的原理是什么？

[返回按分类学习面试题](../README.md)

## 核心结论

`synchronized` 是 Java 内置锁机制，用对象 monitor 实现互斥和可见性。修饰代码块时，字节码会使用
`monitorenter` 和 `monitorexit`；修饰方法时通过方法访问标志表达同步。进入锁前要获取对象 monitor，
退出锁时释放 monitor，并建立 happens-before 关系，保证同步块内写入对后续获取同一锁的线程可见。

## synchronized 锁的是什么？

`synchronized` 锁的是对象。

示例：

```java
synchronized (lock) {
    update();
}
```

这里锁的是 `lock` 对象。

实例同步方法锁的是当前对象 `this`。

```java
public synchronized void update() {
}
```

静态同步方法锁的是类对象。

```java
public static synchronized void refresh() {
}
```

锁的是 `Class` 对象。

## 字节码层原理

同步代码块会编译成类似：

```text
monitorenter
...
monitorexit
```

进入同步块时获取 monitor，正常退出或异常退出都要释放 monitor。

编译器会保证异常路径也执行 `monitorexit`。

## monitor 是什么？

每个 Java 对象都可以关联一个 monitor。

monitor 可以理解为 JVM 层面的锁结构，维护：

- 当前持锁线程。
- 进入计数。
- 等待队列。
- 阻塞线程。

当线程获取不到 monitor 时，会进入阻塞等待。

## 可重入性

`synchronized` 是可重入锁。

同一个线程已经持有某个对象锁时，可以再次进入同一把锁保护的代码。

示例：

```java
synchronized void outer() {
    inner();
}

synchronized void inner() {
}
```

同一线程调用 `outer` 后再进入 `inner` 不会死锁。

monitor 会记录重入次数，退出时逐层释放。

## 可见性

`synchronized` 不只是互斥，也保证可见性。

规则是：

- 释放锁前，会把工作内存中的修改刷新出去。
- 获取同一把锁后，能看到之前释放锁线程的写入。

这对应 Java 内存模型中的 happens-before：

```text
unlock happens-before subsequent lock on the same monitor
```

## 锁升级

HotSpot 曾经有偏向锁、轻量级锁、重量级锁等优化。

Java 17 中偏向锁已经被废弃并默认不可用，但理解锁优化仍有价值。

锁竞争低时，JVM 会尽量用较轻量方式处理；竞争激烈时，会膨胀为更重的 monitor。

面试时不要把旧版本偏向锁细节当成 Java 17 当前默认行为。

# 044 GC Roots 包括哪些？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

GC Roots 是垃圾回收判断对象是否可达的起点。从这些根对象出发能访问到的对象，都不能被回收。
常见 GC Roots 包括线程栈中的局部变量引用、静态字段引用、常量引用、JNI 引用、活跃线程对象、
类加载器相关引用和同步锁持有对象等。

生产排查内存泄漏时，重点不是只看对象大，而是看对象为什么还从 GC Roots 可达。

## 从零基础理解

JVM 判断对象是不是垃圾，不是看有没有变量名，而是看能不能从一组根对象一路找到它。

如果能找到：

```text
GC Root -> A -> B -> LeakedObject
```

那么 `LeakedObject` 不能回收。

如果找不到，它就是不可达对象，可以被回收。

## 常见 GC Roots

### 线程栈中的引用

正在执行的方法局部变量引用的对象。

```java
public void handle() {
    Order order = orderService.get(orderId);
    // order is reachable while method is active.
}
```

### 静态字段

```java
private static final Map<String, Object> CACHE = new HashMap<>();
```

静态集合如果无限增长，很容易导致内存泄漏。

### 常量引用

字符串常量、类常量等可能作为根路径的一部分。

### JNI 引用

native 代码持有的 Java 对象引用。

### 活跃线程

活跃线程本身和它引用的对象都可能成为可达路径。

### 类加载器

类加载器引用类元数据和相关静态对象。ClassLoader 泄漏会导致一批类和对象无法释放。

## 为什么 GC Roots 对排障重要？

内存泄漏的本质通常是：

```text
对象已经不再有业务价值，但仍然被某条 GC Roots 引用链引用。
```

例如：

- 静态 Map 保存请求对象。
- ThreadLocal 没有 remove。
- 监听器注册后未注销。
- 无界队列积压任务。
- 缓存没有 TTL 或最大容量。

## 如何在工具中看？

使用 heap dump 分析工具，例如 MAT、VisualVM、JProfiler。

关注：

- Dominator Tree。
- Retained Size。
- Path to GC Roots。

`Path to GC Roots` 能告诉你对象为什么没被回收。

## 在 eMall 项目中怎么讲？

例如商品详情本地缓存无限增长：

```text
static cache -> product document -> large object graph
```

例如请求上下文 ThreadLocal 未清理：

```text
worker thread -> ThreadLocalMap -> request context -> user/order data
```

这就是生产内存泄漏常见路径。

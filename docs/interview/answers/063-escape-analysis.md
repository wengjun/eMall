# 063 逃逸分析有什么作用？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

逃逸分析是 JIT 判断对象是否会逃出当前方法或当前线程的优化技术。如果对象不会逃逸，JVM 可能做标量替换、
栈上分配或锁消除，从而减少堆分配和 GC 压力。

它的核心价值是让一些看似创建对象的代码，在热点路径上不一定真的产生堆对象。

## 什么是逃逸？

对象创建后，如果能被方法外部访问，就叫逃逸。

示例：

```java
OrderSummary buildSummary(Order order) {
    return new OrderSummary(order.id(), order.totalAmount());
}
```

这里 `OrderSummary` 被返回给调用方，逃出了当前方法。

再看一个例子：

```java
int calculateCents(int price, int quantity) {
    Money money = new Money(price * quantity);
    return money.cents();
}
```

如果 `money` 只在方法内部使用，JIT 可能判断它没有逃逸。

## 逃逸分析能做什么？

逃逸分析本身只是分析，真正收益来自后续优化。

常见优化：

- 标量替换。
- 锁消除。
- 栈上分配。

其中 HotSpot 中最常见、最重要的是标量替换和锁消除。

## 标量替换

如果对象没有逃逸，JIT 可能不真正创建对象，而是把对象字段拆成局部变量。

示例：

```java
record Money(int cents) {
}
```

在热点代码中：

```java
int total(int price, int quantity) {
    Money money = new Money(price * quantity);
    return money.cents();
}
```

JIT 可能把它优化成类似：

```java
int total(int price, int quantity) {
    int cents = price * quantity;
    return cents;
}
```

这样就减少了对象分配。

## 锁消除

如果一个锁对象不会被其他线程访问，JIT 可能消除锁。

示例：

```java
String buildKey(long userId, long skuId) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(userId);
    buffer.append(':');
    buffer.append(skuId);
    return buffer.toString();
}
```

`StringBuffer` 方法有同步，但如果 `buffer` 不逃逸，JIT 可能消除不必要的同步开销。

实际代码中仍建议直接用 `StringBuilder`，不要依赖 JIT 替你修正设计。

## 栈上分配

很多资料会说逃逸分析可以让对象栈上分配。

从理解上可以这样记：不逃逸对象不一定必须进堆。

但面试中更稳妥的说法是：HotSpot 常见优化是标量替换，让对象分配被消除，而不是简单理解成所有对象都放到栈上。

## 对 GC 的价值

对象分配少了，GC 压力就会下降。

尤其在高频接口中，减少临时对象可以降低：

- allocation rate。
- young GC 频率。
- GC CPU。
- 尾延迟抖动。

但不要过度优化。现代 JVM 对短生命周期对象分配和回收很快，应该先通过 profiling 找热点。

## 电商系统实践

价格计算和订单金额计算会创建很多小值对象。

如果这些对象只在方法内部使用，JIT 可能通过逃逸分析和标量替换减少实际分配。

所以在代码设计上可以使用清晰的小对象表达业务含义，不必一开始就为了避免对象而写难维护的过程式代码。
真正需要优化时，再用 JFR 或 allocation profiler 找出分配热点。

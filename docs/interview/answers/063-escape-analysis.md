# 063 逃逸分析有什么作用？

[返回按分类学习面试题](../README.md)

## 题目

逃逸分析有什么作用？

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

## 在 eMall 项目中怎么讲？

价格计算和订单金额计算会创建很多小值对象。

如果这些对象只在方法内部使用，JIT 可能通过逃逸分析和标量替换减少实际分配。

所以在代码设计上可以使用清晰的小对象表达业务含义，不必一开始就为了避免对象而写难维护的过程式代码。
真正需要优化时，再用 JFR 或 allocation profiler 找出分配热点。

## 深度增强：JVM 生产运行图

![Java 17 容器内 JVM 内存结构](../assets/jvm-runtime-memory.svg)

JVM 题要从运行时资源解释到业务影响。堆、直接内存、元空间、线程栈和容器 memory limit 共同决定服务稳定性；
GC、CPU throttling、线程池队列和下游超时会一起影响 P99，而不是孤立存在。

## 深度增强：Java 17 诊断模型示例

```java
record RuntimeSignal(
        double heapUsage,
        double containerMemoryUsage,
        long gcPauseMillis,
        int threadCount,
        int queuedTasks) {

    boolean requiresTriage() {
        return heapUsage > 0.85
                || containerMemoryUsage > 0.90
                || gcPauseMillis > 500
                || threadCount > 800
                || queuedTasks > 1_000;
    }
}
```

这个模型强调线上诊断要看组合信号。只看 heap 不够，只看 GC 也不够；
要把 JVM、容器、线程池和业务延迟放到同一条时间线。

## 深度增强：生产边界

JVM 调优不能靠背参数。要先明确服务目标：低延迟、吞吐、容器资源、对象分配速率和 P99 SLO。
然后通过 GC 日志、JFR、指标和压测验证。错误地调大 `-Xmx` 可能挤压堆外内存，导致容器 OOMKilled。

## 深度增强：面试高分表达

我会用证据链回答 JVM 问题：先看业务影响，再看 JVM 指标、GC 日志、线程栈、heap dump、容器事件和最近变更。
结论要能解释现象，并能给出降级、扩容、参数调整或代码优化方案。

## 专家级完整回答

```text
逃逸分析是 JIT 判断对象是否逃出方法或线程的技术。如果对象没有逃逸，JVM 可以做标量替换、
锁消除等优化，把对象字段拆成局部变量，或者消除不必要同步，从而减少堆分配和 GC 压力。

工程上我不会因为担心小对象就牺牲模型可读性。现代 JVM 对短生命周期对象很友好，只有当
JFR 或 allocation profile 证明对象分配成为热点时，才针对性优化。
```

## 回答评分点

高分答案应该覆盖：

- 逃逸是对象被外部访问。
- 逃逸分析服务于 JIT 优化。
- 标量替换和锁消除是重点。
- 能说清对 GC 压力的影响。
- 不把所有优化都简单说成栈上分配。

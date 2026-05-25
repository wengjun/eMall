# 042 堆、栈、方法区、直接内存分别存什么？

[返回按分类学习面试题](../README.md)

## 题目

堆、栈、方法区、直接内存分别存什么？

## 先给面试官的短答案

堆主要存对象实例和数组；栈是线程私有的，存方法调用栈帧、局部变量和操作数栈；
方法区或元空间存类元数据、方法信息、常量等；直接内存是堆外内存，常用于 NIO、Netty、
文件和网络缓冲。

生产中要记住：Pod 或进程占用内存不是只有堆，直接内存、线程栈、元空间也会占用总内存。

## 堆存什么？

堆存对象实例：

```java
Order order = new Order(...);
String value = new String("abc");
List<Order> orders = new ArrayList<>();
```

对象本身和数组一般都在堆上。

堆的典型问题：

- 对象过多导致堆 OOM。
- 大对象频繁分配导致 GC 压力。
- 缓存不清理导致内存泄漏。
- 集合无限增长。

电商例子：

- 一次查询返回过多订单。
- 本地缓存无限放商品详情。
- MQ 消费者积压对象在内存队列中。

## 栈存什么？

栈是线程私有。每个方法调用创建一个栈帧。

栈帧里包含：

- 局部变量，例如 `int quantity`、`long orderId`。
- 对象引用，例如 `Order order` 这个引用。
- 操作数栈。
- 方法返回信息。

注意：局部变量中的对象引用在栈上，但对象本身通常在堆上。

栈的典型问题：

- 递归太深导致 `StackOverflowError`。
- 线程太多导致无法创建新线程。
- 每个线程栈过大，容器内存被吃掉。

## 方法区或元空间存什么？

方法区是 JVM 规范概念。HotSpot Java 8 以后主要用元空间存类元数据。

包括：

- 类名。
- 方法信息。
- 字段信息。
- 注解元数据。
- 常量池相关信息。
- 类加载器相关数据。

典型问题：

- 动态生成类太多。
- ClassLoader 泄漏。
- 热部署不释放旧类。
- 代理类过多。

## 直接内存存什么？

直接内存是 JVM 堆外的本地内存。

常见使用：

```java
ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
```

Netty、NIO、文件传输、网络 IO、压缩库都可能用直接内存。

优点：

- 某些 IO 场景减少堆和 native 之间复制。
- 适合高性能网络传输。

风险：

- 不在 heap dump 里直观看到。
- 容易被忽略。
- 容器中仍然计入进程内存。

## 面试常见误区

### 引用在栈上，对象在堆上

```java
Order order = new Order(...);
```

`order` 这个局部变量引用在栈上，`new Order(...)` 创建的对象在堆上。

### 堆外内存也会导致容器 OOM

即使 heap used 不高，Pod 也可能因为直接内存、线程栈、元空间过大被杀。

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
堆存对象实例和数组，是 GC 主要管理区域；栈是线程私有的，存方法调用栈帧、
局部变量和对象引用；方法区在 HotSpot Java 8 之后主要由元空间实现，存类元数据；
直接内存是堆外内存，常用于 NIO、Netty 和网络文件缓冲。

生产排障不能只看堆。容器 memory limit 覆盖整个进程内存，线程栈、元空间、直接内存都会计入。
所以 Java 服务设置内存时，要给堆外、线程栈和元空间留余量。
```

## 回答评分点

高分答案应该覆盖：

- 堆存对象，栈存调用帧和引用。
- 元空间存类元数据。
- 直接内存用于 NIO/Netty。
- 能指出引用和对象位置差异。
- 能联系容器总内存和 OOM 排查。

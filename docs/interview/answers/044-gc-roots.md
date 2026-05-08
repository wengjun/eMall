# 044 GC Roots 包括哪些？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

GC Roots 包括哪些？

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
GC Roots 是可达性分析的起点，常见包括线程栈局部变量、静态字段、常量、JNI 引用、
活跃线程、类加载器相关引用和锁持有对象等。

排查内存泄漏时，我不会只看哪个对象大，而会看 Path to GC Roots。
如果对象已经没有业务价值但仍然被静态缓存、ThreadLocal、无界队列或 ClassLoader 引用，
它就无法被回收。
```

## 回答评分点

高分答案应该覆盖：

- GC Roots 是可达性起点。
- 能列出线程栈、静态字段、JNI、类加载器等。
- 能解释对象可达就不能回收。
- 能联系内存泄漏排查。
- 能提到 Path to GC Roots。

## 深度完善：面向 L6 的回答框架

围绕「GC Roots 包括哪些？」，高分答案不能停在概念定义，而要把「内存模型、GC、线程、JIT、诊断命令和容器资源边界」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「JVM 和性能诊断」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `gateway、order、payment、search 在高峰期的 P99、GC、线程池和容器资源` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：P50/P95/P99、GC pause、allocation rate、线程数、CPU throttle、RSS。
- 验证证据：JFR、GC log、jstack、jcmd、heap dump、压测报告和发布对比曲线。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：GC Roots 包括哪些？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

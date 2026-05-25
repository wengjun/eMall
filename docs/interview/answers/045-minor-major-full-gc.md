# 045 Minor GC、Major GC、Full GC 有什么区别？

[返回按分类学习面试题](../README.md)

## 题目

Minor GC、Major GC、Full GC 有什么区别？

## 先给面试官的短答案

Minor GC 通常指年轻代回收；Major GC 通常指老年代回收，但不同资料和收集器中含义可能不完全一致；
Full GC 通常指对整个堆以及方法区或元空间相关区域进行更全面的回收。

面试时要避免死背名词，重点讲清楚：年轻代回收频繁且通常较快，老年代或 Full GC 成本更高，
对服务延迟影响更明显。

## 从零基础理解

Java 对象很多是“朝生夕死”的。例如一次 HTTP 请求里的 DTO、临时字符串、计算中间对象。
所以 JVM 通常把新对象先放年轻代，频繁清理年轻代。

长期存活的对象，比如缓存、连接池、线程池，会进入老年代。

## Minor GC

Minor GC 通常回收年轻代。

特点：

- 发生频繁。
- 通常速度较快。
- 回收短生命周期对象。
- 仍然可能 Stop-The-World。

如果 Minor GC 太频繁，可能说明：

- 对象分配速率过高。
- 年轻代太小。
- 请求产生大量临时对象。

## Major GC

Major GC 通常指老年代回收。但这个词在不同 GC 日志和资料里可能含义不严谨。

老年代回收通常比年轻代更贵，因为老年代对象更多、存活率更高。

如果老年代持续增长，要警惕：

- 缓存泄漏。
- 集合无限增长。
- 队列积压。
- 晋升过快。

## Full GC

Full GC 通常更重，会尝试回收整个堆，并可能涉及元空间等。

Full GC 频繁通常是危险信号：

- 堆压力大。
- 老年代不足。
- 元空间不足。
- 显式 `System.gc()`。
- 分配大对象失败。

## 对服务延迟的影响

GC 可能导致 Stop-The-World，应用线程暂停。

对电商系统：

- 下单接口 P99 升高。
- 支付回调处理延迟。
- 网关转发抖动。
- MQ 消费延迟增加。

## 在 eMall 项目中怎么讲？

如果大促时商品查询产生大量临时对象，Minor GC 会变频繁。
如果本地缓存商品详情不淘汰，老年代会增长，可能触发 Full GC。
如果 MQ 消费积压到内存队列，也会造成堆压力。

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
Minor GC 通常是年轻代回收，频繁但一般较快；Major GC 通常表示老年代回收，
但不同收集器中术语不完全统一；Full GC 通常是更全面的堆和相关元数据回收，成本最高。

生产中我更关注 GC 日志中的暂停时间、频率、回收前后内存变化和触发原因。
如果 Full GC 频繁，就要分析老年代增长、缓存、队列、对象晋升和元空间情况，
而不是只背 Minor/Major/Full 的定义。
```

## 回答评分点

高分答案应该覆盖：

- Minor GC 年轻代。
- Major GC 老年代但术语不完全统一。
- Full GC 更全面且成本高。
- 能联系 Stop-The-World 和 P99。
- 能提出看 GC 日志和触发原因。

# 044 GC Roots 包括哪些？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

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

![Java 17 容器内 JVM 内存结构](../../assets/jvm-runtime-memory.svg)

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

## 二次深度补强

题目：GC Roots 包括哪些？

二次补强标记：已完成

### 面试官真正想确认的能力

JVM 问题要能从现象走到证据，再从证据走到参数、代码和容量边界。
围绕这道题，要进一步把概念、项目实现、线上风险和验证闭环连起来。

### 深度和广度补充

- 先明确现象：延迟、吞吐、CPU、内存、Full GC、线程阻塞或容器 OOM。
- 再收集证据：GC 日志、JFR、线程栈、堆转储、容器指标和业务指标。
- 随后定位主因：对象分配、锁竞争、缓存膨胀、SQL 慢或下游抖动。
- 最后给出验证闭环：压测、灰度、P99、错误率、回滚和复盘。

### 图片讲解

![二次补强图解](../../assets/jvm-runtime-memory.svg)

- 图中把线程栈、堆、元空间、GC 和容器资源放到同一视角。
- 面试回答要说明每个指标在哪里看、怎么关联、怎么验证。
- 不要只说调大内存，要先证明瓶颈在内存而不是下游或锁。

### Java17 延迟样本建模示例

```java
import java.time.Duration;

public record LatencySample(Duration p99, long heapUsedBytes, long gcPauseMillis) {

    boolean violatesSlo(Duration targetP99) {
        return p99.compareTo(targetP99) > 0 || gcPauseMillis > 200;
    }
}

final class RuntimeTriage {

    String classify(LatencySample sample) {
        if (sample.gcPauseMillis() > 200) {
            return "Investigate allocation rate, heap sizing, and GC logs.";
        }
        if (sample.violatesSlo(Duration.ofMillis(300))) {
            return "Check downstream latency, thread pools, and lock contention.";
        }
        return "Runtime is within the current service objective.";
    }
}
```

### 高分表达要点

- 不要只回答定义，要说明为什么这样设计、在什么条件下失效、如何监控和回滚。
- 把答案和当前电商项目联系起来，例如订单、库存、支付、履约、搜索、风控或发布链路。
- 主动给出边界条件和反例，能让面试官看到你具备生产系统判断力。

## 逐题专项补强

逐题专项补强标记：已完成

### 本题专项切入

- 本题要围绕「GC Roots 包括哪些？」展开，不要只复述分类模板。
- 先把症状量化为 P99、CPU、内存、GC、线程和错误率指标。
- 再说明证据链如何从监控、日志、JFR、堆栈和压测中闭环。

### 专项图解说明

![逐题专项图解](../../assets/loadtest-profiling-loop.svg)

- 这张图用于把「GC Roots 包括哪些？」放回生产链路中理解，重点看入口、状态、数据和恢复闭环。
- 面试时可以先按图说明主路径，再补失败路径、监控指标和回滚手段。

### 贴合本题的实现示例

```java
import java.time.Duration;

public record RuntimeSymptom(Duration p99, double cpuUsage, long gcPauseMillis) {

    boolean needsRuntimeTriage() {
        return p99.toMillis() > 300 || cpuUsage > 0.8 || gcPauseMillis > 200;
    }
}
```

### 进一步追问时的回答边界

- 如果面试官继续追问，要主动说明这个实现是核心模型，不等于完整生产组件。
- 生产级落地还需要接入鉴权、幂等、限流、熔断、监控、告警、灰度和数据修复。
- 回答时把复杂度、失败场景、验证方式和 eMall 项目中的落地位置一起说清楚。

## 面试实战补强

面试实战补强标记：已完成

### 面试追问路线

- 如果线上 P99 突然变差，你怎么证明是 JVM 问题而不是下游问题？
- 你会先看哪些指标，哪些命令或工具能给出证据？
- 调参后如何用灰度和压测证明没有引入新的风险？

### eMall 项目落点

- 可以落到模块：loadtest、reliability、operations、platform-ops。
- 回答「GC Roots 包括哪些？」时，要从这些模块里选一个主链路做例子。
- 讲清入口、状态变化、数据写入、异步事件、失败补偿和观测指标。

### 生产验证指标

- P99 延迟
- GC 暂停时间
- 对象分配速率
- 容器 CPU 和内存水位

### 低分陷阱

- 只背定义，不说明业务场景和失败场景。
- 只讲正常路径，不讲超时、重试、回滚、补偿和监控。
- 只给方案，不给验证指标和取舍边界。

### 30 秒高分收束

这道题我会用 JVM、GC、性能诊断 的视角回答。
先给结论，再给项目例子，然后补失败场景、验证指标和取舍边界。
这样能让面试官看到我不是只会背知识点，而是能把知识点落到生产系统。

## 架构取舍与反驳补强

架构取舍补强标记：已完成

### 先给立场

- 回答「GC Roots 包括哪些？」时，不能只给单一方案，要先说明约束、目标和失败边界。
- 高分回答要让面试官看到你能在正确性、可用性、成本、复杂度和团队能力之间做判断。

### 可选方案对比

- 先扩容：止血快，但不能替代根因定位。
- 调 JVM 参数：可能立刻改善延迟，但错误参数会引入新风险。
- 改代码和架构：长期收益高，但需要压测、灰度和迁移成本。

### 反驳和防守

- 如果面试官问为什么不直接上最复杂方案，可以回答：复杂方案只有在规模和风险证明必要时才值得引入。
- 如果面试官问为什么不用最简单方案，可以回答：简单方案可以做第一期，但必须提前设计观测和迁移边界。
- 我的判断原则是：如果约束不明确，先补齐规模、延迟、可用性、一致性、成本和团队能力，再做选择。

### 决策证据

- P95/P99 延迟
- CPU 和内存水位
- 拒绝率和熔断次数
- 压测容量曲线

### 一句话总结

我会先用简单可靠的方案解决当前确定性问题，同时保留观测、灰度和迁移能力。
当指标证明瓶颈存在，再演进到更复杂的架构，而不是为了显得高级提前复杂化。

## 生产落地验收补强

生产验收补强标记：已完成

### 上线前检查

- 针对「GC Roots 包括哪些？」，先确认它影响的是正确性、稳定性、性能、安全还是成本。
- 确认基线压测、GC 日志、JFR 采样和容器资源限制。
- 变更后对比 P99、GC 暂停、CPU、内存和错误率。

### 灰度和回滚

- 先在测试环境和影子流量中验证，再做 1%、5%、25%、50%、100% 分阶段灰度。
- 每个阶段都设置自动暂停条件和人工回滚负责人。
- 回滚不是只回代码，还要确认配置、数据、缓存、消息和任务状态能一起回到安全状态。

### 监控和验收证据

- 压测报告
- P99 对比曲线
- 容量水位表
- 降级和恢复演练记录

### 面试表达

我不会只说方案能实现，还会说明上线前怎么验收、上线中怎么看指标、出问题怎么回滚。
这能证明我关注的是长期稳定运行，而不是只完成一次功能开发。

## 规模化与成本治理补强

规模成本补强标记：已完成

### 规模化视角

- 回答「GC Roots 包括哪些？」时，要主动放到 10 亿用户、1 亿 DAU、100W 峰值并发的背景下思考。
- 按实例容量、堆大小、对象分配速率和容器限制估算运行时容量。
- P99 和 GC 暂停要绑定业务 SLO，而不是只看平均值。

### 成本治理

- 控制过度预留容量，通过压测曲线找到资源利用率和风险的平衡点。
- 优先修复低效代码和热点对象，再考虑简单扩容。

### 自动化和 owner

- 为关键指标建立看板、告警、owner 和 Runbook。
- 把经验沉淀成自动化检查、流水线门禁或平台能力。

### 面试表达

我会补一句：方案能跑只是第一步，大规模下还要回答容量怎么估、成本怎么控、故障谁负责。
这能体现我不是只会实现单点功能，而是能长期运营一个高并发业务系统。


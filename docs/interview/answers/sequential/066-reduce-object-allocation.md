# 066 如何减少不必要的对象分配？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

如何减少不必要的对象分配？

## 先给面试官的短答案

减少对象分配要先用 JFR 或 allocation profiler 找热点，再针对性优化。常见手段包括避免循环内重复创建对象、
控制集合容量、减少中间集合、优化字符串拼接和日志、分页处理大数据、复用昂贵资源、避免无意义装箱。

原则是先测量，再优化；优先消除真正热点，而不是消灭所有对象。

## 先定位分配热点

不要凭感觉优化。

应该先看：

- 哪些类分配最多。
- 哪些调用栈分配最多。
- allocation rate 是否异常。
- young GC 是否频繁。
- P99 是否与 GC 或分配峰值相关。

工具包括：

- JFR。
- async-profiler allocation mode。
- JDK Mission Control。
- YourKit 或 JProfiler。

## 避免循环内重复创建

循环中重复创建相同对象很常见。

低效示例：

```java
for (OrderLine line : lines) {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
    result.add(formatter.format(line.createdAt()));
}
```

如果对象不可变且线程安全，可以移到循环外或定义为常量。

```java
private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;
```

注意不是所有对象都能共享，必须确认线程安全。

## 控制集合容量

集合扩容会产生额外数组和复制成本。

如果能预估大小，可以指定容量。

```java
List<OrderView> views = new ArrayList<>(orders.size());
```

这适合批量转换、查询结果映射、导出数据准备等场景。

## 减少中间集合

链式流式处理可读性好，但在热点路径中可能创建额外对象。

例如多次 `map`、`filter`、`collect` 可能产生中间对象和 lambda 开销。

如果 profiling 证明它是热点，可以改成单次循环。

但不要全局禁用 Stream。非热点代码中，可读性更重要。

## 优化字符串和日志

字符串分配是常见热点。

注意：

- 避免在循环中使用低效拼接。
- 日志使用占位符。
- DEBUG 日志前判断是否启用。
- 避免记录超大对象。
- JSON 序列化避免重复转换。

示例：

```java
log.debug("Create order request userId={}, skuId={}", userId, skuId);
```

不要提前拼接：

```java
log.debug("Create order request userId=" + userId + ", skuId=" + skuId);
```

## 避免无意义装箱

频繁装箱会创建对象。

例如：

```java
Long total = 0L;
for (long value : values) {
    total += value;
}
```

热点代码中可以使用 primitive：

```java
long total = 0L;
```

集合泛型无法存 primitive 时，可以考虑专门集合库，但要权衡依赖和复杂度。

## 分页和流式处理

一次性加载大量数据会产生大量对象。

更好的方式：

- 分页查询。
- 游标处理。
- 流式导出。
- 批次提交。
- 限制最大返回条数。

电商后台导出订单、对账、报表都需要避免一次性全量加载。

## 复用昂贵资源

不是所有对象都需要复用。

适合复用的是昂贵资源：

- 线程池。
- 数据库连接。
- HTTP 连接。
- 大 direct buffer。
- JSON mapper。
- 正则 Pattern。

普通业务 DTO 不建议复杂复用。

## 在 eMall 项目中怎么讲？

如果营销规则计算分配过高，优化步骤应该是：

- 用 JFR 找到分配最多的类和栈。
- 看是否循环内重复创建规则上下文。
- 看是否反复序列化商品和用户画像。
- 看是否多次构造中间集合。
- 对热点路径做容量预估、缓存不可变对象和减少中间对象。

这样比盲目重写所有代码更有效。

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
减少对象分配必须先 profiling。确认 allocation hotspot 后，我会从循环内重复创建、集合扩容、
中间集合、字符串和日志、装箱、大对象加载、JSON 重复序列化等方向优化。对于连接、线程、
Pattern、ObjectMapper 这类昂贵对象可以复用，但普通业务 DTO 不建议复杂对象池化。

目标不是消灭对象，而是降低真正影响 GC 和 P99 的分配速率，同时保持代码可读性。
```

## 回答评分点

高分答案应该覆盖：

- 先用工具定位热点。
- 优化循环、集合容量、中间集合、字符串和装箱。
- 分页处理大数据。
- 区分普通对象和昂贵资源。
- 强调不要盲目对象池。

## 深度完善：面向 L6 的回答框架

围绕「如何减少不必要的对象分配？」，高分答案不能停在概念定义，而要把「内存模型、GC、线程、JIT、诊断命令和容器资源边界」讲成一条可验证的工程链路。
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

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：如何减少不必要的对象分配？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。


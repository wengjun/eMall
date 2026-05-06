# 073 GC 日志如何阅读？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

GC 日志如何阅读？

## 先给面试官的短答案

读 GC 日志要看时间、GC 类型、触发原因、暂停时间、回收前后内存变化、各代空间变化和频率。
核心问题是判断 GC 是否频繁、暂停是否影响 P99、old gen 是否能回落、是否有 Full GC 或 humongous allocation。

GC 日志要和接口延迟、CPU、容器内存、发布和流量变化一起看。

## 先看哪些字段？

常见关注点：

- 发生时间。
- GC 类型。
- 触发原因。
- pause duration。
- heap before。
- heap after。
- young/old 区域变化。
- user/sys/real 时间。

不要只看单条日志，要看一段时间内的趋势。

## GC 类型

不同收集器日志不同，但通常要识别：

- young GC。
- mixed GC。
- old GC。
- Full GC。
- concurrent cycle。
- evacuation pause。

对 G1 来说，young GC 和 mixed GC 比较常见。

Full GC 通常是更严重信号，需要重点关注。

## 看暂停时间

暂停时间直接影响接口延迟。

如果 GC pause 与 P99 峰值时间一致，GC 可能是延迟根因之一。

要关注：

- 单次最大暂停。
- pause P99。
- 单位时间总暂停。
- 是否有连续长暂停。

单次 100 ms 对某些后台任务没问题，但对支付或下单接口可能已经明显影响用户体验。

## 看回收效果

GC 前后内存变化很关键。

如果 young GC 后 heap 明显下降，说明很多对象正常死亡。

如果 old gen 持续上涨，且多次 GC 后不回落，可能是：

- 长生命周期对象增多。
- 缓存无上限。
- 队列堆积。
- 内存泄漏。
- 大对象晋升。

回收效果比单纯 heap 使用率更重要。

## 看 GC 频率

频繁 young GC 可能表示 allocation rate 高。

频繁 Full GC 可能表示老年代压力过大或元空间问题。

要问：

- 每秒发生多少次 GC？
- 每分钟 GC 总暂停多长？
- GC 是否越来越频繁？
- 是否从 young GC 发展到 mixed GC 或 Full GC？

频率和趋势能反映系统是否接近失稳。

## 看触发原因

触发原因能帮助定位。

常见原因：

- allocation failure。
- humongous allocation。
- metadata GC threshold。
- system GC。
- evacuation failure。

如果看到 `System.gc()` 触发，要检查业务代码或第三方库。

如果看到 humongous allocation，要检查大数组、大字符串、大 JSON 或批量查询。

## user/sys/real 时间

日志中可能有：

```text
User=0.20s Sys=0.02s Real=0.05s
```

含义：

- User：用户态 CPU 时间。
- Sys：内核态 CPU 时间。
- Real：真实墙钟时间。

如果 Real 明显大于 User+Sys，可能有 CPU 不足、容器 throttling 或系统调度问题。

## 在 eMall 项目中怎么讲？

订单创建 P99 抖动时，可以对比 GC 日志和接口延迟。

如果 P99 高峰时正好出现长 GC pause，并且 old gen 持续上涨，就要进一步分析 heap dump。

如果日志显示 humongous allocation，可能是订单导出、营销规则或大 JSON 响应导致。

如果 GC 正常，则要转向线程池、数据库、锁竞争和下游慢。

## 深度增强：GC 日志与延迟图

![GC 暂停与尾延迟放大](../../assets/gc-pause-latency.svg)

读 GC 日志的关键不是背字段，而是回答三个问题：是否暂停太久，是否回收有效，是否和业务延迟峰值对齐。
如果 GC 日志显示 pause 很长，但业务 P99 没变化，它可能不是当前用户影响的主因。

## 深度增强：Java 17 GC 日志摘要模型

```java
import java.time.Instant;

record GcEvent(
        Instant time,
        String type,
        String reason,
        long pauseMillis,
        long heapBeforeMb,
        long heapAfterMb) {

    long reclaimedMb() {
        return heapBeforeMb - heapAfterMb;
    }

    boolean ineffective() {
        return pauseMillis > 200 && reclaimedMb() < heapBeforeMb * 0.05;
    }
}
```

这个模型表达 GC 分析思路：pause 是用户影响，before/after 是回收效果，reason 是定位方向。
如果长暂停回收很少，要怀疑老年代压力、内存泄漏、缓存无界或大对象晋升。

## 深度增强：生产边界

GC 日志要看一段时间的趋势。一次 young GC 长暂停可能是偶发，频繁 mixed GC 或 Full GC 才说明系统接近失稳。
如果看到 `humongous allocation`，要检查大数组、大 JSON、批量查询和大字符串拼接。

Java 17 使用统一日志，建议同时打开 `gc` 和 `safepoint`。有时候应用暂停不是 GC 本身，
而是 safepoint、偏向锁撤销、类卸载或 JVM 内部操作。只开普通 GC 指标可能看不全。

## 深度增强：面试高分表达

我会按时间、类型、原因、暂停、回收前后内存、频率和 user/sys/real 来读 GC 日志。
然后把这些信息和 P99、CPU、容器 throttling、发布和流量峰值对齐。我的目标不是解释每一行日志，
而是判断 GC 是否是业务延迟或 OOM 风险的证据。

## 专家级完整回答

```text
读 GC 日志我会看时间、GC 类型、触发原因、暂停时间、GC 前后 heap 和 old gen 变化、GC 频率、
以及 user/sys/real。核心判断是：GC 是否影响 P99，old gen 是否能回落，是否出现 Full GC、
System.gc、humongous allocation 或 evacuation failure。

GC 日志不能孤立看，要和接口延迟、QPS、CPU、容器 throttling、发布和业务活动时间线对齐。
```

## 回答评分点

高分答案应该覆盖：

- 看类型、原因、暂停、前后内存。
- 关注 old gen 是否回落。
- 关注 Full GC 和 humongous allocation。
- 理解 user/sys/real。
- 能把 GC 和 P99 时间线对齐。
## 深度完善：专项验收清单

围绕「GC 日志如何阅读？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
回答时要把概念、代码、数据、失败路径和指标串起来，证明自己不是只理解单点知识。

### 项目落点

- 先说明它在 eMall 哪个模块或链路中出现，例如交易、库存、支付、搜索、风控、发布或可观测性。
- 再说明它保护的核心目标：正确性、可用性、延迟、成本、安全或协作效率。
- 最后补失败场景：超时、重试、重复请求、状态不一致、热点流量、配置错误或发布回滚。

### 验收证据

- 代码证据：关键类、状态机、唯一约束、事务边界、线程池隔离或配置项。
- 测试证据：单元测试、集成测试、契约测试、压测、故障注入或回归用例。
- 运行证据：指标看板、Trace、结构化日志、告警、Runbook、对账结果或补偿记录。

### 高分收束

面试最后要回到取舍：当前方案为什么足够简单可靠，什么时候需要升级，升级时如何灰度、回滚和验证。
这样回答能体现生产系统判断力，而不是只罗列技术名词。

深度完善标记：专题增强答案已补项目落点、验收证据和取舍收束。

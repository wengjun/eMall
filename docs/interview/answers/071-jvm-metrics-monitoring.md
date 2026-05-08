# 071 如何做 JVM 指标监控？

[返回按分类学习面试题](../README.md)

## 题目

如何做 JVM 指标监控？

## 先给面试官的短答案

JVM 指标监控要覆盖内存、GC、线程、类加载、JIT、直接内存、进程资源和应用线程池。
生产上通常通过 Micrometer、Prometheus、Grafana、JMX Exporter 或 APM 采集，并把 JVM 指标和接口延迟、
错误率、流量、下游依赖放在同一张时间线上分析。

JVM 监控不是为了看图，而是为了提前发现泄漏、GC 抖动、线程堆积和资源耗尽。

## 为什么要监控 JVM？

Java 微服务运行在 JVM 上，很多故障会先表现为 JVM 指标异常。

例如：

- heap 持续上涨。
- old gen 回收不下来。
- GC pause 变长。
- 线程数持续增加。
- direct memory 上涨。
- 类加载数量异常。
- CPU 被 GC 消耗。

如果没有 JVM 指标，只能等用户报慢或服务 OOM 后再排查。

## 采集方式

常见采集方式：

- Spring Boot Actuator + Micrometer。
- Prometheus scrape `/actuator/prometheus`。
- JMX Exporter。
- APM Agent。
- OpenTelemetry metrics。
- 云厂商监控。

Spring Boot 微服务中，Micrometer + Prometheus 是很常见的组合。

## 指标分层

不要只监控 heap。

JVM 指标至少分为：

- 内存指标。
- GC 指标。
- 线程指标。
- 类加载指标。
- direct buffer 指标。
- 进程 CPU 和内存。
- 应用线程池指标。
- HTTP client 和数据库连接池指标。

JVM 问题经常和应用资源池一起出现。

## 告警设计

告警不能只设一个固定阈值。

更合理的告警包括：

- old gen 使用率持续高。
- Full GC 次数增加。
- GC pause P99 超标。
- thread count 持续上涨。
- direct memory 接近上限。
- heap used 持续上涨且无法回落。
- 容器 memory working set 接近 limit。
- 线程池队列持续堆积。

告警要关注持续时间和趋势，避免瞬时波动误报。

## 关联业务指标

JVM 指标必须和业务指标关联。

例如：

- GC pause 是否对应接口 P99 峰值。
- heap 上涨是否对应活动开始。
- 线程数上涨是否对应下游超时。
- direct memory 上涨是否对应上传流量。
- CPU 高是否对应规则计算 QPS。

只看 JVM 指标无法判断业务影响。

## 仪表盘设计

一个实用 JVM 仪表盘可以包括：

- QPS、错误率、P99。
- heap、non-heap、direct memory。
- young GC、old GC 次数和耗时。
- GC pause histogram。
- thread count 和 thread states。
- CPU usage 和 load。
- class loaded count。
- process memory 和 container memory。
- 线程池 active、queue、rejected。

面试中说出这些维度，会比只说“监控 GC 和内存”更完整。

## 在 eMall 项目中怎么讲？

eMall 的订单、支付、库存、网关都应该有 JVM 监控。

例如订单服务要重点看：

- heap used。
- GC pause。
- order-create P99。
- 业务线程池队列。
- 数据库连接池。
- 下游 HTTP client。

网关还要重点看 direct memory、连接数和 event loop 延迟。

## 深度增强：JVM 指标监控图

![Java 17 容器内 JVM 内存结构](../assets/jvm-runtime-memory.svg)

JVM 监控要覆盖堆、非堆、直接内存、线程、GC 和容器总内存。单看 heap 很容易误判：
heap 稳定但 container memory 上涨，可能是 direct memory、线程栈、metaspace 或 native memory。

## 深度增强：Java 17 指标快照模型

```java
import java.time.Instant;

record JvmMetricSnapshot(
        Instant time,
        long heapUsedBytes,
        long heapMaxBytes,
        long nonHeapUsedBytes,
        long directUsedBytes,
        int threadCount,
        long gcPauseMillis,
        long containerMemoryBytes) {

    double heapUsage() {
        return heapMaxBytes == 0 ? 0 : (double) heapUsedBytes / heapMaxBytes;
    }

    boolean risky() {
        return heapUsage() > 0.85
                || threadCount > 800
                || gcPauseMillis > 1_000;
    }
}
```

这个模型可以帮助学习者理解监控维度。真实系统中这些指标通常来自 Micrometer、JMX、Prometheus、
APM 和容器监控。告警不应只看瞬时值，还要看持续时间和趋势。

## 深度增强：生产边界

JVM 指标必须和业务指标一起看。old gen 高但 P99、错误率和 GC pause 都正常，可能只是缓存预热；
GC pause 和订单创建 P99 同步升高，才说明有明确业务影响。

告警设计要避免“阈值噪声”。例如 heap 使用率瞬间超过 85% 不一定要报警，但 old gen 多次 GC 后不回落、
GC pause P99 持续超标、线程池队列持续堆积，这些趋势更值得报警。

## 深度增强：面试高分表达

我会说 JVM 监控不是为了看 JVM，而是为了提前发现业务风险。监控要覆盖 heap、direct memory、GC、
线程、类加载、CPU、container memory、线程池和连接池，并和 QPS、错误率、P99、下游延迟放在同一时间线。
这样才能判断 JVM 异常是否真的影响交易。

## 专家级完整回答

```text
JVM 监控我会用 Micrometer/Prometheus/APM 采集，覆盖 heap、non-heap、direct memory、GC、
线程、类加载、CPU、进程内存和应用线程池。告警不只看瞬时阈值，还要看持续趋势，比如 old gen
回收不下来、GC pause P99 超标、线程数持续上涨、容器 memory 接近 limit。

更重要的是把 JVM 指标和 QPS、错误率、P99、下游依赖、连接池放到同一时间线，判断是否真的影响业务。
```

## 回答评分点

高分答案应该覆盖：

- 监控不只 heap。
- 采集方式能说出 Micrometer、Prometheus、JMX 或 APM。
- 告警要看趋势和持续时间。
- JVM 指标要关联业务指标。
- 能提到线程池和连接池。
## 深度完善：专项验收清单

围绕「如何做 JVM 指标监控？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

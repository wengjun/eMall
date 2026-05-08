# 056 如何判断是 GC、数据库、锁竞争还是下游慢导致延迟升高？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

如何判断是 GC、数据库、锁竞争还是下游慢导致延迟升高？

## 先给面试官的短答案

要用时间线对齐和链路拆解判断。GC 看 pause 时间是否和延迟峰值重合；数据库看慢 SQL、连接池、
锁等待和事务耗时；锁竞争看线程 dump/JFR 中 `BLOCKED`、monitor enter 和锁等待；下游慢看 trace
中对应 span、HTTP client 指标、超时和错误率。

关键不是猜，而是用指标把端到端耗时拆开。

## 统一排查思路

延迟升高可以拆成几类时间：

- 请求排队时间。
- 应用本地执行时间。
- JVM 暂停时间。
- 数据库等待和执行时间。
- 缓存访问时间。
- 下游调用时间。
- 锁等待时间。
- 网络传输时间。

排查就是把总耗时分解到这些部分。

## 判断是否是 GC

GC 导致延迟升高的特征：

- P99 峰值和 GC pause 时间点重合。
- GC pause duration 增大。
- old gen 使用率高。
- allocation rate 异常。
- Full GC 或 mixed GC 频率升高。
- 应用日志中出现短时间整体停顿。

需要看的证据：

- GC 日志。
- JVM metrics。
- JFR GC events。
- heap usage。
- allocation profile。

如果所有接口同时抖动，并且抖动时间和 GC pause 对齐，GC 可能性很高。

## 判断是否是数据库

数据库导致延迟升高的特征：

- 慢 SQL 增多。
- 数据库连接池 active 接近上限。
- 获取连接耗时升高。
- 数据库 CPU 或 IO 升高。
- 行锁等待或事务等待升高。
- trace 中数据库 span 变慢。

需要看的证据：

- SQL 慢日志。
- 连接池指标。
- 数据库监控。
- 执行计划。
- 锁等待视图。
- 事务持续时间。

如果只有依赖数据库的接口慢，而纯缓存接口不慢，数据库可能性更高。

## 判断是否是锁竞争

锁竞争导致延迟升高的特征：

- `jstack` 中大量线程 `BLOCKED`。
- 多个线程等待同一个 monitor。
- JFR 中 Java Monitor Blocked 时间升高。
- CPU 不一定很高，但线程等待很多。
- P99 高但下游 span 不慢。

常见原因：

- 大 synchronized 临界区。
- 锁内访问数据库或下游。
- 本地缓存刷新使用全局锁。
- 热点商品或库存本地串行化。
- 单个对象作为全局锁。

如果 trace 显示外部调用都快，但应用自身耗时很高，要重点看锁竞争和线程池排队。

## 判断是否是下游慢

下游慢导致延迟升高的特征：

- trace 中某个下游 span 明显变长。
- HTTP client pending request 增加。
- 下游错误率或超时上升。
- 调用方线程卡在 socket read 或 client execute。
- 下游连接池 active 接近上限。
- 熔断器慢调用比例升高。

需要看的证据：

- 链路追踪。
- 下游 SLA 监控。
- HTTP client 指标。
- 连接池指标。
- 超时和重试指标。
- 调用方 `jstack`。

下游慢最危险的是重试放大和线程占满，所以要配合熔断、降级和隔离。

## 对比表

| 根因 | 关键证据 | 常见工具 |
| --- | --- | --- |
| GC | pause 与 P99 对齐 | GC log、JFR、JVM metrics |
| 数据库 | 慢 SQL、连接池满、锁等待 | 慢日志、连接池指标、执行计划 |
| 锁竞争 | 大量 `BLOCKED` 或 monitor blocked | `jstack`、JFR |
| 下游慢 | trace 下游 span 变长 | Trace、HTTP client metrics |

## 时间线对齐很关键

生产排查不要孤立看指标。

要把这些时间点放到同一张时间线上：

- P99 峰值时间。
- GC pause 时间。
- 慢 SQL 时间。
- 下游超时时间。
- 发布变更时间。
- 线程池队列上涨时间。
- 错误率上涨时间。

谁先发生，谁更可能是根因。

## 在 eMall 项目中怎么讲？

订单创建变慢时：

- 如果所有接口同时卡顿，看 GC。
- 如果只有创建订单和查询订单慢，看订单库和连接池。
- 如果秒杀商品订单慢，普通商品不慢，看库存热点锁。
- 如果支付前置校验慢，看支付或风控下游 span。
- 如果 trace 没有下游慢但 order span 很长，看线程池排队和锁竞争。

这样回答能体现你不是凭经验猜，而是按证据链定位。

## 深度增强：统一时间线图

![SRE 延迟和 CPU 故障排查时间线](../assets/sre-triage-timeline.svg)

延迟排查最怕“看到哪个指标异常就认定它是根因”。正确做法是用同一时间线对齐：
P99 什么时候升高，GC 什么时候暂停，数据库慢 SQL 什么时候出现，下游错误率什么时候升高，
最近发布和配置变更是什么时候发生。

## 深度增强：Java 17 延迟归因模型

```java
import java.time.Duration;

record LatencyBreakdown(
        Duration queueTime,
        Duration appTime,
        Duration gcPause,
        Duration databaseTime,
        Duration lockWait,
        Duration downstreamTime) {

    Duration total() {
        return queueTime
                .plus(appTime)
                .plus(gcPause)
                .plus(databaseTime)
                .plus(lockWait)
                .plus(downstreamTime);
    }

    String primarySuspect() {
        Duration max = queueTime;
        String suspect = "queue";
        if (databaseTime.compareTo(max) > 0) {
            max = databaseTime;
            suspect = "database";
        }
        if (downstreamTime.compareTo(max) > 0) {
            max = downstreamTime;
            suspect = "downstream";
        }
        if (gcPause.compareTo(max) > 0) {
            suspect = "gc";
        }
        return suspect;
    }
}
```

这段代码把端到端耗时拆成多个部分，表达排障思想：先拆解，再比较，再用证据验证。
真实系统中的这些数据来自 trace span、JVM 指标、连接池指标、JFR 和业务埋点。

## 深度增强：生产边界

根因可能不止一个。下游慢会导致线程池队列堆积，队列堆积会导致 heap 上升，heap 上升会导致 GC 变慢，
GC 变慢又会放大 P99。排查时要找到最早异常和放大链路，而不是只处理最后一个现象。

对核心交易链路，建议把订单创建拆成网关排队、鉴权、风控、库存、价格、数据库事务、消息发送和下游调用。
每段都有独立指标，事故时才能快速判断是本地问题还是依赖问题。

## 深度增强：面试高分表达

我会回答“用证据链拆延迟”。GC 看 pause 与 P99 是否对齐，数据库看慢 SQL 和连接池，
锁竞争看 JFR 和大量 BLOCKED，下游慢看 trace span 和 client 指标。谁先异常、谁能解释后续现象，
谁才更接近根因。

## 专家级完整回答

```text
我会把端到端延迟拆成排队、应用执行、GC 暂停、数据库、锁等待和下游调用。GC 看 pause 时间
是否和 P99 峰值对齐；数据库看慢 SQL、连接池、执行计划和锁等待；锁竞争看 jstack/JFR 中
大量 BLOCKED 或 monitor blocked；下游慢看 trace 的 span、HTTP client 指标、超时和错误率。

真正生产排查的关键是时间线对齐，而不是单点猜测。哪个指标先异常，且能解释后续现象，
哪个更接近根因。
```

## 回答评分点

高分答案应该覆盖：

- 能拆解端到端耗时。
- GC 看 pause 与 P99 是否对齐。
- 数据库看慢 SQL、连接池和锁等待。
- 锁竞争看 `jstack` 和 JFR。
- 下游慢看 trace span 和 client 指标。
- 强调时间线和证据链。
## 深度完善：专项验收清单

围绕「如何判断是 GC、数据库、锁竞争还是下游慢导致延迟升高？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

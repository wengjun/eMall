# 638 手写滑动窗口限流器。

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

手写滑动窗口限流器。

## 先给面试官的短答案

现场编码要先澄清输入输出、边界和复杂度，再写最小正确实现，并说明生产化改造。

## 核心拆解

- 先澄清输入、输出、异常和并发边界。
- 用 Java 17 或 SQL 写小而正确的核心实现。
- 补充复杂度、测试用例和失败场景。
- 说明面试实现与生产实现的差异。

## 深度增强：图解

![](../../assets/coding-patterns.svg)

这张图用于把问题放到生产系统中理解。面试时不要只讲单点技术，而要说明它在容量、稳定性、
一致性、可观测性和故障恢复中的位置。

## 深度增强：Java 17 或 SQL 示例

```java
import java.util.LinkedHashMap;
import java.util.Map;

final class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    LruCache(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
```

## 生产边界和常见坑

这个问题的关键不是“能不能做”，而是能否在高并发、灰度发布、故障恢复和数据修复场景下安全运行。
如果方案缺少监控、限流、幂等、回滚、审计或补偿，就只能算 demo，不能算生产级方案。

## 在 eMall 项目中怎么讲？

可以结合 eMall 的 `gateway`、`order`、`inventory`、`payment`、`risk`、`traffic`、
`reliability`、`release`、`operations` 和 `analytics` 模块说明。核心表达是：
先保护交易主链路，再保证数据可追踪，最后通过观测、补偿和复盘把风险沉淀为平台能力。

## 专家级完整回答

```text
我会先明确这个问题影响的是容量、可用性、一致性、安全还是工程效率。
然后拆解核心链路和失败场景，给出当前规模下最务实的方案。
生产系统里我会同时设计指标、告警、灰度、回滚、审计和补偿，避免方案只在正常路径成立。
如果规模继续增长，我会再从分片、异步化、多区域、自动化治理和成本优化上演进。
```

## 回答评分点

- 能先讲业务目标和生产影响。
- 能拆解核心链路、数据流和失败场景。
- 能给出 Java 17、SQL 或工程实现示例。
- 能说明监控、告警、回滚、补偿和审计。
- 能结合 eMall 项目说明落地方式。

## 二次深度补强

题目：手写滑动窗口限流器。

二次补强标记：已完成

### 面试官真正想确认的能力

编码题要讲清数据结构、复杂度、边界条件、测试用例和可维护性。
围绕这道题，要进一步把概念、项目实现、线上风险和验证闭环连起来。

### 深度和广度补充

- 先复述题意和输入输出，确认边界条件。
- 再选择数据结构，并说明时间复杂度和空间复杂度。
- 随后写出清晰 Java17 代码，变量名表达业务含义。
- 最后补充测试用例，包括正常、边界、异常和大数据量。

### 图片讲解

![二次补强图解](../../assets/coding-patterns.svg)

- 图中展示从问题建模、代码实现、测试覆盖到工程质量的闭环。
- 读图时要说明编码不是写完就结束，还要可测、可读、可扩展。
- 高分回答要主动讨论复杂度和失败输入。

### Java17 滑动窗口限流示例

```java
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;

final class SlidingWindowRateLimiter {
    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final Deque<Long> hits = new ArrayDeque<>();

    SlidingWindowRateLimiter(int limit, long windowMillis, Clock clock) {
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    synchronized boolean allow() {
        long now = clock.millis();
        while (!hits.isEmpty() && now - hits.peekFirst() >= windowMillis) {
            hits.removeFirst();
        }
        if (hits.size() >= limit) {
            return false;
        }
        hits.addLast(now);
        return true;
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

- 本题要围绕「手写滑动窗口限流器。」展开，不要只复述分类模板。
- 先确认输入输出和边界，再说明数据结构、复杂度和失败用例。
- 代码要能在 Java17 下独立表达核心思路，并说明可测性。

### 专项图解说明

![逐题专项图解](../../assets/rate-limit-circuit-breaker.svg)

- 这张图用于把「手写滑动窗口限流器。」放回生产链路中理解，重点看入口、状态、数据和恢复闭环。
- 面试时可以先按图说明主路径，再补失败路径、监控指标和回滚手段。

### 贴合本题的实现示例

```java
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;

final class SlidingWindowRateLimiter {
    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final Deque<Long> hits = new ArrayDeque<>();

    SlidingWindowRateLimiter(int limit, long windowMillis, Clock clock) {
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    synchronized boolean allow() {
        long now = clock.millis();
        while (!hits.isEmpty() && now - hits.peekFirst() >= windowMillis) {
            hits.removeFirst();
        }
        if (hits.size() >= limit) {
            return false;
        }
        hits.addLast(now);
        return true;
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

- 输入规模和边界条件是什么，空值、重复值、并发和异常如何处理？
- 时间复杂度、空间复杂度和可读性如何权衡？
- 如果要上线到生产系统，还需要补哪些监控、测试和保护措施？

### eMall 项目落点

- 可以落到模块：common、traffic、inventory、event-platform。
- 回答「手写滑动窗口限流器。」时，要从这些模块里选一个主链路做例子。
- 讲清入口、状态变化、数据写入、异步事件、失败补偿和观测指标。

### 生产验证指标

- 限流拒绝率
- 熔断打开次数
- 半开恢复成功率
- 核心链路错误率

### 低分陷阱

- 只写出 happy path，没有边界条件和复杂度分析。
- 代码不可测试，依赖当前时间、随机数或全局状态。
- 没有说明生产化还缺少哪些保护。

### 30 秒高分收束

这道题我会用 现场编码和设计实现 的视角回答。
先给结论，再给项目例子，然后补失败场景、验证指标和取舍边界。
这样能让面试官看到我不是只会背知识点，而是能把知识点落到生产系统。

## 架构取舍与反驳补强

架构取舍补强标记：已完成

### 先给立场

- 回答「手写滑动窗口限流器。」时，不能只给单一方案，要先说明约束、目标和失败边界。
- 高分回答要让面试官看到你能在正确性、可用性、成本、复杂度和团队能力之间做判断。

### 可选方案对比

- 入口限流：保护整体容量，但可能误伤部分真实用户。
- 线程池隔离：保护调用方资源，但需要额外容量规划和拒绝策略。
- 熔断降级：能快速止血，但需要半开探测和平滑恢复避免二次雪崩。

### 反驳和防守

- 如果面试官问为什么不直接上最复杂方案，可以回答：复杂方案只有在规模和风险证明必要时才值得引入。
- 如果面试官问为什么不用最简单方案，可以回答：简单方案可以做第一期，但必须提前设计观测和迁移边界。
- 我的判断原则是：如果影响系统稳定性，优先保护核心链路和整体可用性，而不是追求单次请求成功。

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

- 针对「手写滑动窗口限流器。」，先确认它影响的是正确性、稳定性、性能、安全还是成本。
- 确认限流阈值、熔断窗口、半开探测、降级文案和白名单策略。
- 灰度期间观察拒绝率、恢复成功率和核心链路错误率。

### 灰度和回滚

- 先说明核心算法只解决题目本身，生产化必须增加监控、限流和测试。
- 如果代码涉及并发，要先小流量验证再扩大使用范围。
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

- 回答「手写滑动窗口限流器。」时，要主动放到 10 亿用户、1 亿 DAU、100W 峰值并发的背景下思考。
- 按入口峰值、下游容量、线程池容量和排队长度共同设置保护阈值。
- 容量策略要支持按用户、商家、SKU、接口和机房分层保护。

### 成本治理

- 说明算法的时间和空间成本。
- 生产化时要避免无限集合、无限队列和无限重试。

### 自动化和 owner

- 为关键指标建立看板、告警、owner 和 Runbook。
- 把经验沉淀成自动化检查、流水线门禁或平台能力。

### 面试表达

我会补一句：方案能跑只是第一步，大规模下还要回答容量怎么估、成本怎么控、故障谁负责。
这能体现我不是只会实现单点功能，而是能长期运营一个高并发业务系统。


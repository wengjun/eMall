# 048 如何判断线上服务是否存在内存泄漏？

[返回按分类学习面试题](../README.md)

## 题目

如何判断线上服务是否存在内存泄漏？

## 先给面试官的短答案

判断内存泄漏不能只看内存高，要看 GC 后内存基线是否持续上升、老年代是否持续增长、
Full GC 后是否无法回落、对象数量是否持续增加，以及 heap dump 中对象是否通过异常引用链被持有。

核心判断是：对象已经没有业务价值，但仍然从 GC Roots 可达，导致无法回收。

## 现象

常见现象：

- heap used 持续上升。
- Old Gen 持续上升。
- Full GC 后内存不明显下降。
- GC 越来越频繁。
- P99 变差。
- 最终 OOM。
- Pod 被 OOMKilled。

但内存高不一定是泄漏，也可能是缓存预热、流量增长或堆设置合理利用。

## 判断步骤

### 1. 看趋势

观察较长时间：

- GC 后基线是否上升。
- 老年代是否持续上升。
- 流量下降后是否回落。

### 2. 看 GC 日志

关注：

- Full GC 频率。
- Full GC 前后内存。
- 触发原因。
- 回收效果。

### 3. 导出 heap dump

使用 `jcmd`、`jmap` 或平台工具导出。

### 4. 分析对象

用 MAT、JProfiler、VisualVM 看：

- Dominator Tree。
- Retained Size。
- 对象数量。
- Path to GC Roots。

### 5. 找引用链

内存泄漏的关键是找到谁还持有对象。

## 常见泄漏来源

- 静态 Map 无限增长。
- 本地缓存没有容量和 TTL。
- ThreadLocal 未 remove。
- 线程池队列无界。
- MQ 消费积压到内存。
- 监听器注册后未注销。
- ClassLoader 泄漏。
- 大结果集一次性加载。
- 日志异步队列堵塞。

## 在 eMall 项目中怎么讲？

可能场景：

- 商品详情本地缓存没有淘汰。
- Outbox 待发布事件加载到内存过多。
- MQ 消费者失败后把消息放入无界内存队列。
- 请求上下文 ThreadLocal 没有清理。
- 压测工具记录所有请求结果不释放。

## 深度增强：JVM 内存结构图

![Java 17 容器内 JVM 内存结构](../assets/jvm-runtime-memory.svg)

内存泄漏排查要先明确泄漏发生在哪里。堆泄漏通常能在 heap dump 中看到对象和引用链；
直接内存、线程栈、元空间和 native 内存问题，单靠 heap dump 不一定能解释容器内存上涨。

## 深度增强：Java 17 泄漏与修复示例

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class UnsafeRequestContextHolder {
    private static final ThreadLocal<Map<String, String>> CONTEXT = new ThreadLocal<>();

    void handle(Map<String, String> context, Runnable action) {
        CONTEXT.set(new ConcurrentHashMap<>(context));
        try {
            action.run();
        } finally {
            CONTEXT.remove();
        }
    }
}
```

`ThreadLocal` 常见泄漏不是因为它不能用，而是线程池线程会复用。如果请求结束后不 `remove`，
旧请求上下文可能一直挂在线程上。生产代码要用 `try-finally` 保证清理，尤其是网关、鉴权和日志 MDC。

## 深度增强：生产判断方法

判断泄漏要看“GC 后基线”。如果流量下降后，Full GC 后 old gen 仍持续抬高，才更像泄漏。
如果只是活动期间缓存预热、热点商品增多或流量上涨导致 heap 高，可能不是泄漏。

实际排查顺序可以是：先看 heap used、old gen、GC pause 和容器 memory；再导出 heap dump；
用 Dominator Tree 找 retained size 最大对象；最后看 Path to GC Roots，确认是哪条引用链让对象不可回收。

## 深度增强：面试高分表达

我不会看到内存高就说泄漏。我的判断标准是对象已经没有业务价值，但仍从 GC Roots 可达。
证据上看 GC 后基线、old gen 趋势和 heap dump 引用链；修复上处理无界缓存、无界队列、
ThreadLocal、监听器和大批量加载。这样回答能体现我会用证据定位，而不是靠经验猜。

## 如何修复？

- 给缓存加最大容量和 TTL。
- 使用有界队列。
- ThreadLocal 用完 remove。
- 大批量查询分页处理。
- 限制一次加载数量。
- 对消费者做背压。
- 清理监听器。

## 专家级完整回答

```text
我判断内存泄漏会先看趋势，而不是只看内存高。
如果 Full GC 后老年代基线持续上升，流量下降后也不回落，就要怀疑泄漏。
然后导出 heap dump，用 MAT 看 Dominator Tree、Retained Size 和 Path to GC Roots，
找出对象为什么仍然可达。

常见原因包括静态缓存、ThreadLocal、无界队列、MQ 积压和 ClassLoader 泄漏。
修复上要加 TTL、容量限制、remove、分页和背压。
```

## 回答评分点

高分答案应该覆盖：

- 区分内存高和泄漏。
- 看 GC 后基线和老年代趋势。
- 使用 heap dump 和 Path to GC Roots。
- 能说常见泄漏来源。
- 能提出修复措施。
## 深度完善：专项验收清单

围绕「如何判断线上服务是否存在内存泄漏？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

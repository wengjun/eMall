# 250 如何设计限流？

[返回按分类学习面试题](../README.md)

## 题目

如何设计限流？

## 先给面试官的短答案

限流是保护系统在超过承载能力时有序拒绝请求，而不是等资源耗尽后整体崩溃。
设计限流要明确保护对象、限流维度、算法、阈值来源、返回策略、监控告警和灰度调整。

限流既要保护服务，也要尽量保证高价值流量优先通过。

## 保护对象

限流可以保护：

- 网关。
- 单个服务。
- 数据库。
- 缓存。
- MQ。
- 第三方接口。
- SKU 或商家热点。

保护对象不同，限流位置和维度也不同。

## 限流维度

常见维度：

- 全局 QPS。
- 用户。
- IP。
- 设备。
- 商家。
- SKU。
- API。
- appKey。

开放平台通常按 appKey 和接口配额限流，秒杀通常按活动和 SKU 限流。

## 返回策略

被限流后要：

- 快速失败。
- 返回明确错误码。
- 告诉客户端是否可重试。
- 可以返回 `Retry-After`。
- 对核心业务提供排队或降级。

不能让请求在服务内部长时间排队。

## 在 eMall 项目中怎么讲？

秒杀活动要在网关按活动和用户限流，在库存服务按 SKU 限流，在数据库前保护热点库存写入。

被限流用户收到明确提示，而不是让请求进入订单服务后超时。

## 深度增强：限流位置图

![限流和熔断在调用链中的位置](../assets/rate-limit-circuit-breaker.svg)

限流的本质不是“让用户排队等更久”，而是把超过承载能力的请求挡在合适的位置。
越靠入口的限流越适合保护整体容量，越靠业务内部的限流越适合保护热点资源。

## 深度增强：Java 17 令牌桶实现

下面是单机令牌桶的核心实现。生产中网关或 Redis 可以实现分布式限流，但算法思想一致。

```java
public final class TokenBucketRateLimiter {

    private final long capacity;
    private final long refillTokensPerSecond;
    private long availableTokens;
    private long lastRefillNanos;

    public TokenBucketRateLimiter(long capacity, long refillTokensPerSecond) {
        if (capacity <= 0 || refillTokensPerSecond <= 0) {
            throw new IllegalArgumentException("Capacity and refill rate must be positive.");
        }
        this.capacity = capacity;
        this.refillTokensPerSecond = refillTokensPerSecond;
        this.availableTokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    public synchronized boolean tryAcquire(long requiredTokens) {
        refill();
        if (requiredTokens <= availableTokens) {
            availableTokens -= requiredTokens;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillNanos;
        long tokensToAdd = elapsedNanos * refillTokensPerSecond / 1_000_000_000L;
        if (tokensToAdd > 0) {
            availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
            lastRefillNanos = now;
        }
    }
}
```

这段代码面试时要主动说明边界：

- 单机限流只保护单个实例，不能控制集群总 QPS。
- 分布式限流可用 Redis Lua、网关控制面或服务网格实现。
- 阈值不能拍脑袋，要来自压测、容量估算和生产指标。
- 限流结果要有明确错误码，并尽量返回 `Retry-After`。
- 重点用户、支付回调、内部补偿任务可以有不同优先级和配额。

## 深度增强：面试高分表达

```text
我会先问限流要保护什么。如果保护入口，就在网关按 IP、用户、appKey 和 API 做限流；
如果保护热点资源，就在服务内按 SKU、商家或下游依赖做限流。算法上令牌桶适合允许一定突发，
滑动窗口适合精确统计窗口内请求数。阈值来自压测和容量评估，限流结果必须快速失败，
不能让请求在内部排队把线程池和连接池耗尽。
```

## 专家级完整回答

```text
限流要先明确保护对象和限流维度。网关适合做入口、用户、IP、appKey 限流；
服务内适合按业务资源限流，例如 SKU、商家或下游依赖。

限流算法可以用令牌桶、漏桶或滑动窗口。阈值要通过容量评估和压测确定，并配合监控、灰度和动态调整。
```

## 回答评分点

高分答案应该覆盖：

- 限流是保护系统有序拒绝。
- 要明确保护对象和维度。
- 网关和服务内限流职责不同。
- 返回要快速且可理解。
- 阈值要基于容量和压测。
## 深度完善：专项验收清单

围绕「如何设计限流？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

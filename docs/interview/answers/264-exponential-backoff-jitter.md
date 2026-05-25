# 264 什么是指数退避和 jitter？

[返回按分类学习面试题](../README.md)

## 题目

什么是指数退避和 jitter？

## 先给面试官的短答案

指数退避是每次重试等待时间按指数增长，例如 `100ms`、`200ms`、`400ms`。
jitter 是在等待时间上加入随机抖动，避免大量客户端在同一时间同时重试。

退避降低下游压力，jitter 防止重试流量同步冲击。

## 指数退避

示例：

```text
第 1 次重试：100ms
第 2 次重试：200ms
第 3 次重试：400ms
第 4 次重试：800ms
```

通常还要设置最大等待时间和最大重试次数。

## jitter

如果所有客户端都按固定 `400ms` 重试，会形成波峰。

jitter 会让等待时间随机分散：

```text
等待时间 = baseDelay + random(0, jitterRange)
```

这样重试请求不会集中打到下游。

## 常见策略

策略包括：

- 固定退避。
- 指数退避。
- Full jitter。
- Equal jitter。
- Decorrelated jitter。

工程上不一定追求复杂算法，但必须避免同步重试。

## 在 eMall 项目中怎么讲？

订单服务调用支付查询接口失败后，可以按 `100ms`、`200ms`、`400ms` 退避，并加入随机抖动。

这样支付服务恢复时不会被所有订单请求同时打爆。

## 深度增强：数据访问和扩展图

![数据库、缓存和消息一致性链路](../assets/data-cache-mq.svg)

数据库题要从访问路径、索引、锁、事务和容量出发。电商系统的数据层既要支撑高并发读写，
又要保证订单、库存、支付等事实数据可追踪。缓存和消息可以提升性能，但不能替代数据库事实来源。

## 深度增强：Java 17 数据访问策略示例

```java
record QueryPlan(String accessPath, boolean usesIndex, boolean requiresPagination) {

    boolean safeForOnlineTraffic() {
        return usesIndex && requiresPagination;
    }
}

final class OnlineQueryPolicy {

    void verify(QueryPlan plan) {
        if (!plan.safeForOnlineTraffic()) {
            throw new IllegalArgumentException("Online query must use index and pagination");
        }
    }
}
```

这段代码体现线上查询治理：不是 SQL 能跑就可以上线，而是要确认走索引、可分页、可限流、可观测。

## 深度增强：生产边界

核心表设计要从典型查询倒推索引，避免全表扫描、深分页和大事务。分库分表要先选好分片键，
避免跨分片事务和热点分片。任何数据迁移都要支持灰度、校验、回滚或修复。

## 深度增强：面试高分表达

我会从访问模式回答数据题：谁查、按什么条件查、QPS 多少、数据量多大、是否强一致、是否需要分页和排序。
然后再决定索引、分片、缓存、读写分离和归档策略。

## 专家级完整回答

```text
指数退避是让重试间隔逐次增大，给下游恢复时间；jitter 是在重试等待时间中加入随机扰动，
避免大量请求在同一时刻重试形成流量尖峰。

生产系统的重试应同时设置最大次数、最大延迟和总 deadline，并结合熔断和限流。
```

## 回答评分点

高分答案应该覆盖：

- 指数退避让等待时间逐次增长。
- jitter 用于打散重试流量。
- 二者降低重试风暴风险。
- 要设置最大次数和最大等待。
- 总耗时不能超过 deadline。

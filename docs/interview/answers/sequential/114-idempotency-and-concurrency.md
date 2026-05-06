# 114 如何设计幂等和并发安全的组合方案？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

如何设计幂等和并发安全的组合方案？

## 先给面试官的短答案

组合方案通常包括幂等 key、唯一键或幂等表、状态机条件更新、事务边界、消息 outbox、重试策略和补偿机制。
并发安全保证同一时刻不会写错，幂等保证重复请求返回同一结果。两者要一起设计。

电商核心链路不能只靠锁，必须有资源端约束和业务状态机。

## 幂等 key

每个请求需要稳定唯一标识。

来源可以是：

- 客户端请求号。
- 支付流水号。
- 订单号。
- 消息 ID。
- 业务组合键。

没有幂等 key，就很难识别重复请求。

## 幂等表

幂等表记录请求处理状态：

```text
request_id
status
result
created_at
updated_at
```

状态可以是：

- PROCESSING。
- SUCCESS。
- FAILED。

并发请求先尝试插入幂等记录，只有成功者执行业务。

## 状态机条件更新

业务状态推进要带条件。

示例：

```sql
update orders
set status = 'PAID'
where order_id = ? and status = 'UNPAID'
```

这能防止重复支付回调重复推进状态。

## 唯一键兜底

唯一键是资源端最后防线。

例如：

- `client_request_id unique`。
- `payment_no unique`。
- `user_id + coupon_id unique`。
- `message_id unique`。

即使应用层并发控制失效，数据库仍能防重复。

## Outbox

本地事务内同时写业务表和 outbox 事件表。

事务提交后由后台发布消息。

这样能避免业务成功但消息发送失败。

消费者再用幂等保证重复消息安全。

## 重试和补偿

重试必须：

- 只重试幂等操作。
- 有次数限制。
- 有退避和抖动。
- 能查询已有结果。

补偿用于处理半成功状态，例如库存已扣但订单创建失败。

## 在 eMall 项目中怎么讲？

创建订单可以这样设计：

- 客户端传 `requestId`。
- 订单表对 `requestId` 建唯一键。
- 库存扣减使用条件更新。
- 订单状态用状态机推进。
- 订单创建成功写 outbox。
- 重复请求查询并返回已有订单。

这样能同时抗并发、抗重试和抗消息重复。

## 深度增强：并发治理图

![Java 并发从线程安全到容量保护](../../assets/concurrency-governance.svg)

并发题不能只回答 API 用法。生产系统要同时考虑线程安全、资源隔离、超时、拒绝、幂等和分布式多实例。
单机锁只能保护当前 JVM，不能保护整个集群；线程池满也不是小问题，而是容量和可用性风险。

## 深度增强：Java 17 有界并发示例

```java
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

final class BulkheadGuard {
    private final Semaphore permits;

    BulkheadGuard(int maxConcurrentCalls) {
        this.permits = new Semaphore(maxConcurrentCalls);
    }

    <T> T execute(Supplier<T> supplier) {
        if (!permits.tryAcquire()) {
            throw new IllegalStateException("Bulkhead rejected the call");
        }
        try {
            return supplier.get();
        } finally {
            permits.release();
        }
    }
}
```

这段代码展示了并发控制的生产思路：不是让所有请求无限进入系统，而是在入口保护共享资源。
真实项目还要加超时、指标、降级和按下游隔离。

## 深度增强：生产边界

线程安全不等于系统安全。`ConcurrentHashMap` 只能保护当前进程内的数据结构，
不能替代数据库唯一键、幂等表或分布式一致性。线程池也不能使用无界队列，
否则会把过载转化成内存上涨和 P99 恶化。

## 深度增强：面试高分表达

我会把并发问题分成三层：JMM 和锁保证单机正确性，线程池和隔离保证资源不被拖垮，
幂等和唯一键保证分布式正确性。这样能体现我理解 Java 并发，也理解微服务生产稳定性。

## 专家级完整回答

```text
幂等和并发安全要组合设计。入口用稳定幂等 key，资源端用唯一键或幂等表防重复，业务推进用状态机条件更新，
本地事务写 outbox 保证消息可靠，消费者再做消息幂等。重试必须有次数、退避和查询已有结果能力。

锁只能减少冲突，不能作为最终正确性保证。最终要靠数据库约束、状态机、幂等记录和补偿闭环。
```

## 回答评分点

高分答案应该覆盖：

- 幂等 key。
- 唯一键或幂等表。
- 状态机条件更新。
- Outbox 和消息幂等。
- 重试和补偿。

## 二次深度补强

题目：如何设计幂等和并发安全的组合方案？

二次补强标记：已完成

### 面试官真正想确认的能力

并发题要同时回答正确性、吞吐、隔离、超时、取消和故障恢复。
围绕这道题，要进一步把概念、项目实现、线上风险和验证闭环连起来。

### 深度和广度补充

- 先定义共享资源和临界区，明确并发冲突在哪里发生。
- 再选择工具：锁、CAS、队列、线程池、信号量、幂等或分布式锁。
- 随后补齐失败路径：超时、重试、取消、锁过期、线程池耗尽和降级。
- 最后用压测和可观测指标证明方案能承受峰值流量。

### 图片讲解

![二次补强图解](../../assets/concurrency-governance.svg)

- 图中展示入口限流、线程池隔离、锁保护和降级之间的关系。
- 读图时要关注请求什么时候被拒绝，什么时候排队，什么时候快速失败。
- 高分回答要能说明保护的是系统稳定性，而不是单次请求成功率。

### Java17 有界异步执行示例

```java
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

final class BoundedAsyncExecutor implements AutoCloseable {
    private final Semaphore permits = new Semaphore(128);
    private final ExecutorService pool = Executors.newFixedThreadPool(32);

    <T> CompletableFuture<T> submit(Callable<T> task) {
        if (!permits.tryAcquire()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Too many requests."));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                permits.release();
            }
        }, pool);
    }

    @Override
    public void close() {
        pool.shutdown();
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

- 本题要围绕「如何设计幂等和并发安全的组合方案？」展开，不要只复述分类模板。
- 先确定共享资源、并发冲突点、超时策略和隔离边界。
- 再说明高峰流量下如何限流、排队、快速失败和恢复。

### 专项图解说明

![逐题专项图解](../../assets/concurrency-governance.svg)

- 这张图用于把「如何设计幂等和并发安全的组合方案？」放回生产链路中理解，重点看入口、状态、数据和恢复闭环。
- 面试时可以先按图说明主路径，再补失败路径、监控指标和回滚手段。

### 贴合本题的实现示例

```java
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class IdempotencyProcessor {
    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    boolean processOnce(String requestId, Runnable action) {
        if (!processed.add(requestId)) {
            return false;
        }
        action.run();
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

- 这个方案在 100W 最高并发下如何避免线程池、队列或锁成为雪崩点？
- 如果锁过期、任务超时、线程被中断或请求重试，会不会破坏正确性？
- 你如何设计拒绝策略、超时策略、降级策略和监控指标？

### eMall 项目落点

- 可以落到模块：risk、identity、openapi、gateway。
- 回答「如何设计幂等和并发安全的组合方案？」时，要从这些模块里选一个主链路做例子。
- 讲清入口、状态变化、数据写入、异步事件、失败补偿和观测指标。

### 生产验证指标

- 线程池活跃数
- 队列长度
- 拒绝次数
- 锁等待时间

### 低分陷阱

- 只背定义，不说明业务场景和失败场景。
- 只讲正常路径，不讲超时、重试、回滚、补偿和监控。
- 只给方案，不给验证指标和取舍边界。

### 30 秒高分收束

这道题我会用 并发、线程池、分布式锁 的视角回答。
先给结论，再给项目例子，然后补失败场景、验证指标和取舍边界。
这样能让面试官看到我不是只会背知识点，而是能把知识点落到生产系统。

## 架构取舍与反驳补强

架构取舍补强标记：已完成

### 先给立场

- 回答「如何设计幂等和并发安全的组合方案？」时，不能只给单一方案，要先说明约束、目标和失败边界。
- 高分回答要让面试官看到你能在正确性、可用性、成本、复杂度和团队能力之间做判断。

### 可选方案对比

- 网关前置校验：能尽早拦截风险，但规则过重会增加入口延迟。
- 服务内精细鉴权：表达能力强，但容易分散导致策略不一致。
- 异步风控：主链路快，但对高风险交易需要同步拦截或二次验证。

### 反驳和防守

- 如果面试官问为什么不直接上最复杂方案，可以回答：复杂方案只有在规模和风险证明必要时才值得引入。
- 如果面试官问为什么不用最简单方案，可以回答：简单方案可以做第一期，但必须提前设计观测和迁移边界。
- 我的判断原则是：如果涉及安全边界，优先选择默认拒绝、最小权限、可审计和可轮换的方案。

### 决策证据

- 拦截率和误杀率
- 审计日志覆盖率
- 密钥轮换记录
- 越权和重放攻击拦截数

### 一句话总结

我会先用简单可靠的方案解决当前确定性问题，同时保留观测、灰度和迁移能力。
当指标证明瓶颈存在，再演进到更复杂的架构，而不是为了显得高级提前复杂化。

## 生产落地验收补强

生产验收补强标记：已完成

### 上线前检查

- 针对「如何设计幂等和并发安全的组合方案？」，先确认它影响的是正确性、稳定性、性能、安全还是成本。
- 确认默认拒绝、最小权限、密钥轮换、审计日志和风控误杀恢复。
- 上线前要做越权、重放、签名篡改和权限回归测试。

### 灰度和回滚

- 先在测试环境和影子流量中验证，再做 1%、5%、25%、50%、100% 分阶段灰度。
- 每个阶段都设置自动暂停条件和人工回滚负责人。
- 回滚不是只回代码，还要确认配置、数据、缓存、消息和任务状态能一起回到安全状态。

### 监控和验收证据

- 安全回归测试
- 审计日志样例
- 误杀恢复记录
- 密钥轮换记录

### 面试表达

我不会只说方案能实现，还会说明上线前怎么验收、上线中怎么看指标、出问题怎么回滚。
这能证明我关注的是长期稳定运行，而不是只完成一次功能开发。

## 规模化与成本治理补强

规模成本补强标记：已完成

### 规模化视角

- 回答「如何设计幂等和并发安全的组合方案？」时，要主动放到 10 亿用户、1 亿 DAU、100W 峰值并发的背景下思考。
- 按入口请求量、验签 CPU 成本、风控规则复杂度和审计写入量估算容量。
- 安全链路要避免把重计算全部压在网关同步路径。

### 成本治理

- 用单位成本看问题，例如单请求成本、单订单成本、单消息成本和单 GB 存储成本。
- 先优化浪费最高的环节，而不是平均用力。

### 自动化和 owner

- 为关键指标建立看板、告警、owner 和 Runbook。
- 把经验沉淀成自动化检查、流水线门禁或平台能力。

### 面试表达

我会补一句：方案能跑只是第一步，大规模下还要回答容量怎么估、成本怎么控、故障谁负责。
这能体现我不是只会实现单点功能，而是能长期运营一个高并发业务系统。


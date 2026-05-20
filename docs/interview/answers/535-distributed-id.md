# 535 设计分布式 ID 服务

[返回按分类学习面试题](../README.md)

## 题目

设计分布式 ID 服务。

## 先给面试官的短答案

分布式 ID 服务要生成全局唯一、趋势递增、高性能、可用性高、可反解部分信息的 ID。常见方案有数据库号段、
Snowflake、Redis 自增和 UUID。电商订单号通常需要高吞吐、趋势递增和业务可读性，可以使用号段或 Snowflake 变体。

## 需求

ID 生成要满足全局唯一、低延迟、高可用、可水平扩展和不依赖单点。订单号还需要避免暴露敏感信息，
并支持按时间粗略排序，方便分库分表和排查。

不同业务可以使用不同 ID。订单号、支付单号、用户 ID、消息 ID 不一定使用同一个生成策略。

## 方案比较

UUID 简单但太长、无序，对数据库索引不友好。Redis 自增简单，但 Redis 可用性和持久化要谨慎。

数据库号段通过一次取一批 ID 到本地缓存，性能高、趋势递增、实现稳定，但需要处理号段耗尽和服务重启浪费。

Snowflake 通过时间戳、机器号和序列号生成 ID，性能高、无中心依赖，但要处理时钟回拨和机器号分配。

## 生产设计

如果使用 Snowflake，要监控时钟回拨，机器号不能冲突。可以通过注册中心或配置中心分配 workerId。

如果使用号段，要双 buffer 预取，避免号段耗尽时阻塞。号段表要高可用，步长要根据业务峰值调整。

## 在 eMall 项目中怎么讲？

eMall 的订单、支付、退款、履约、消息都需要稳定 ID。订单号可以用时间前缀加分布式序列，内部主键可以用 Long ID。
对外展示 ID 和内部数据库 ID 可以分离，避免暴露分片规则。

## 深度增强：ID 结构图

![Snowflake 变体 ID 结构](../assets/distributed-id.svg)

分布式 ID 不是只要唯一就行。订单、支付、消息、用户对 ID 的要求不同：
内部主键关注索引友好和路由，对外订单号关注可读性、客服查询和不暴露敏感信息。

## 深度增强：Java 17 Snowflake 变体

```java
public final class SnowflakeIdGenerator {

    private static final long EPOCH = 1_700_000_000_000L;
    private static final long WORKER_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    private final long workerId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(long workerId) {
        long maxWorkerId = (1L << WORKER_BITS) - 1;
        if (workerId < 0 || workerId > maxWorkerId) {
            throw new IllegalArgumentException("Invalid workerId.");
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards.");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << (WORKER_BITS + SEQUENCE_BITS))
                | (workerId << SEQUENCE_BITS)
                | sequence;
    }

    private long waitNextMillis(long previousTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= previousTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
```

## 深度增强：生产边界

- workerId 必须统一分配，不能两个实例拿到同一个 workerId。
- 时钟回拨要告警，不能静默生成重复 ID。
- 同毫秒序列耗尽时要等待下一毫秒或扩展序列位。
- 对外订单号和内部主键建议分离，避免暴露分片和业务规模。
- ID 服务要监控生成 QPS、时钟回拨、worker 冲突和号段剩余量。

## 深度增强：面试高分表达

```text
我不会直接说用 Snowflake。先看业务诉求：订单 ID 要全局唯一、趋势递增、索引友好、可排查；
对外订单号还要避免暴露分片规则。Snowflake 性能高，但要处理 workerId 冲突和时钟回拨；
号段方案稳定，但要双 buffer 和步长治理。不同业务可以选择不同 ID 策略。
```

## 专家级完整回答

```text
分布式 ID 设计要先看业务诉求，而不是直接选一个算法。

对电商订单，我需要全局唯一、趋势递增、高性能和可排查。可以选数据库号段或 Snowflake 变体。
号段方案中心化但稳定，Snowflake 性能高但要处理时钟回拨和机器号冲突。

我会把对外订单号和内部主键分开设计。内部 ID 服务分库分表和索引，对外订单号服务客服、用户和审计。
```

## 回答评分点

高分答案应该覆盖：

- 覆盖全局唯一、趋势递增、高性能和高可用。
- 能比较 UUID、Redis、号段和 Snowflake。
- 知道 Snowflake 的时钟回拨和 workerId 问题。
- 知道号段的双 buffer 和步长设计。
- 能结合订单号和内部主键分离说明。
## 深度完善：专项验收清单

围绕「设计分布式 ID 服务」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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

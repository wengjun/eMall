# 378 单分区热点如何处理？

[返回按分类学习面试题](../README.md)

## 题目

单分区热点如何处理？

## 先给面试官的短答案

单分区热点通常由分区键倾斜导致，例如所有秒杀 SKU 消息都使用同一个 `skuId` 作为 key。处理方式
包括调整分区键、热点 key 加桶、拆分 Topic、业务削峰、异步聚合和专门的热点处理链路。

核心是分散热点，同时明确顺序性会被如何影响。

## 发现方式

表现：

- 某个 Partition lag 远高于其他 Partition。
- 某个 Consumer 负载很高。
- Topic 总 lag 不大但最大分区 lag 很大。
- 某个业务 key 请求量异常。

要看 Partition 维度，而不是只看总量。

## 治理方式

方式：

- 调整 message key。
- 对热点 key 加 bucket。
- 增加 Partition 并重分布。
- 单独拆热点 Topic。
- 秒杀请求先削峰。
- 消费端按桶并行处理。

如果必须保证单 key 严格顺序，分桶会破坏这个顺序，需要业务兜底。

## 分桶示例

原 key：

```text
skuId
```

分桶 key：

```text
skuId + ":" + bucketNo
```

这样可以把同一热点 SKU 分散到多个 Partition。

## 在 eMall 项目中怎么讲？

eMall 秒杀库存事件如果都按 `skuId` 分区，爆品会压垮单个 Partition。

可以按库存桶或令牌桶编号分区，例如 `skuId:bucketNo`。最终库存一致性由库存桶汇总、条件扣减和
对账补偿保证，而不是依赖单 Partition 串行。

## 深度增强：缓存和消息治理图

![数据库、缓存和消息一致性链路](../assets/data-cache-mq.svg)

缓存和消息题要关注一致性、削峰、延迟、积压和恢复。
Redis 很快，但会遇到穿透、击穿、雪崩、热点 key 和内存淘汰；
MQ 能解耦和削峰，但会带来重复消费、乱序、积压和死信处理。

## 深度增强：Java 17 幂等消费示例

```java
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class LocalIdempotentConsumer {
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    boolean tryHandle(String messageKey, Runnable handler) {
        if (!processedKeys.add(messageKey)) {
            return false;
        }
        handler.run();
        return true;
    }
}
```

这个示例只适合解释幂等思想。生产环境不能用本地内存做全局幂等，要使用数据库唯一键、Redis 原子操作或业务状态机。

## 深度增强：生产边界

缓存要有 TTL、容量、降级和回源保护；消息要有重试、死信、延迟队列、消费幂等和积压告警。
缓存不一致要能修复，消息失败要能回放，不能只依赖人工查日志。

## 深度增强：面试高分表达

我会把缓存和消息都看成性能与稳定性工具，而不是正确性事实来源。
正确性由数据库事实、状态机、幂等和对账保证；缓存和 MQ 负责降低延迟、削峰填谷和解耦系统。

## 专家级完整回答

```text
单分区热点通常是分区键倾斜。处理上可以调整 key、对热点 key 分桶、拆分热点 Topic、增加 Partition、
业务削峰和消费端并行化。

但要注意，分桶会牺牲原来单 key 的严格顺序。电商秒杀场景通常用库存桶、令牌和最终对账来换取
并发能力，而不是让所有请求在一个 Partition 串行。
```

## 回答评分点

高分答案应该覆盖：

- 单分区热点来自 key 倾斜。
- 看最大分区 lag。
- 调整 key 或热点分桶。
- 分桶会影响顺序。
- 秒杀要结合削峰和库存桶。

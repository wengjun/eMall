# 376 消息积压如何排查？

[返回按分类学习面试题](../README.md)

## 题目

消息积压如何排查？

## 先给面试官的短答案

消息积压要先判断是生产突增还是消费变慢，再按 Topic、Consumer Group、Partition、下游依赖和
变更时间线定位。核心指标是生产速率、消费速率、lag、处理耗时、错误率和 Rebalance。

排查目标不是只把 lag 降下来，还要防止重复消费、下游雪崩和数据不一致。

## 排查步骤

步骤：

- 确认哪个 Topic 和 Group 积压。
- 查看是否所有 Partition 都积压。
- 对比生产速率和消费速率。
- 查看消费错误日志。
- 查看下游服务和数据库延迟。
- 检查最近发布和配置变更。
- 检查 Rebalance 和消费者存活。

先定位范围，再处理。

## 常见原因

原因：

- 流量突增。
- 消费者实例减少。
- 下游接口慢。
- 数据库慢 SQL。
- 单 Partition 热点。
- poison message 反复失败。
- Rebalance 频繁。
- offset 提交异常。

不同原因处理方式不同。

## 处理方式

方式：

- 临时扩容消费者。
- 增加 Partition。
- 批量处理。
- 降低下游调用频率。
- 慢消息送重试或死信。
- 对非核心消费降级。
- 为核心 Topic 单独资源隔离。

处理时要注意幂等和顺序。

## 在 eMall 项目中怎么讲？

eMall 履约消费者积压时，要先看订单创建流量是否突增，再看仓储系统接口是否变慢。

如果是仓储接口慢，盲目扩容消费者会把仓储打垮。更好的方式是限速消费、排队、重试和保护履约
下游，同时让订单前台展示处理中状态。

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
消息积压要比较生产速率和消费速率，定位是流量突增还是消费变慢。再按 Topic、Group、Partition
查看 lag 分布，并关联消费者错误、下游延迟、数据库慢 SQL、Rebalance 和发布变更。

处理可以扩容、批量化、增加 Partition、隔离慢消息和限速下游，但必须保护幂等、顺序和下游容量。
```

## 回答评分点

高分答案应该覆盖：

- 先判断生产突增还是消费变慢。
- 按 Topic、Group、Partition 定位。
- 关注下游和数据库。
- 慢消息隔离。
- 处理时保护幂等和下游。

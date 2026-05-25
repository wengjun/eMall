# 343 热 key 如何发现？

[返回按分类学习面试题](../README.md)

## 题目

热 key 如何发现？

## 先给面试官的短答案

热 key 可以从客户端埋点、Redis 代理层统计、Redis 自身命令、慢日志、网络流量、业务活动信息和
监控告警中发现。生产中最好在访问入口就统计 key 维度 QPS 和命中率。

热 key 发现要尽量实时，因为热点往往在活动、爆品和故障期间突然出现。

## 发现方式

方式包括：

- 客户端 SDK 统计 key 访问频率。
- Redis Proxy 统计。
- Redis `--hotkeys`。
- 慢日志和延迟监控。
- 网络流量采样。
- 业务活动配置提前识别。
- 分片 CPU 和流量异常反推。

单靠事后日志通常太慢。

## 关键指标

指标：

- 单 key QPS。
- key 命中率。
- 单分片 CPU。
- Redis 带宽。
- P99 延迟。
- 连接数。
- 慢命令数量。

热 key 通常表现为某个分片负载远高于其他分片。

## 提前发现

提前发现来自业务信息：

- 秒杀 SKU。
- 大促会场。
- 明星商品。
- 推送链接。
- 热搜词。
- 运营配置的重点活动。

技术系统要接收业务热点预告。

## 在 eMall 项目中怎么讲？

eMall 可以在 Redis 客户端封装层统计 key 前缀和完整 key 的访问次数。

例如 `product:detail:v1:sku:10001` 在一分钟内 QPS 突然超过阈值，就标记为热 key，并自动触发
本地缓存、热点副本或限流策略。

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
热 key 发现要结合技术监控和业务预判。技术上可以在客户端、代理层和 Redis 指标中统计单 key
QPS、分片 CPU、带宽、P99 延迟和慢命令。业务上要提前识别秒杀 SKU、活动页和热搜词。

生产中最有效的是在统一 Redis 客户端或代理层做实时统计，这样发现后才能自动触发热点治理。
```

## 回答评分点

高分答案应该覆盖：

- 客户端或代理层统计。
- Redis 自身工具和监控。
- 单 key QPS 和分片负载。
- 业务活动预判。
- 发现要接近实时。

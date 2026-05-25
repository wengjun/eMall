# 346 如何拆分大 key？

[返回按分类学习面试题](../README.md)

## 题目

如何拆分大 key？

## 先给面试官的短答案

拆分大 key 的核心是把一个过大的 value 或集合，按业务维度、分页维度、哈希分桶或时间维度拆成
多个小 key，同时保证读取路径能组合或分页访问。

不能只拆写入，还要重新设计读取、删除、迁移和过期策略。

## 拆分方式

常见方式：

- 按用户或商家拆。
- 按分页拆。
- 按时间拆。
- 按哈希桶拆。
- 只缓存摘要。
- 明细放数据库或搜索系统。

选择方式取决于查询模式。

## 示例

评价列表不要放成：

```text
product:reviews:sku:10001
```

可以拆成：

```text
product:reviews:sku:10001:page:1
product:reviews:sku:10001:page:2
```

如果是成员集合，可以按 hash bucket 拆分。

## 删除和过期

拆分后要考虑：

- 多个 key 的批量删除。
- TTL 一致性。
- 目录 key 或索引 key。
- 异步清理。
- 避免一次删除过多 key。

拆分提高了可扩展性，也增加了管理复杂度。

## 在 eMall 项目中怎么讲？

eMall 购物车可以限制单用户 SKU 数量，并按店铺或分桶拆分购物车 key。

商品评价只缓存第一页和摘要，不缓存全部评价。热销榜可以按类目、城市或时间窗口拆分，而不是
全站一个巨大 ZSet。

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
大 key 拆分要根据访问模式设计。列表型数据可以分页拆，时间序列可以按时间拆，集合型数据可以
按 hash bucket 拆，聚合展示可以只缓存摘要。

拆分后要同步设计读取聚合、批量删除、TTL、目录索引和异步清理。否则只是把一个大问题变成多个
小问题。
```

## 回答评分点

高分答案应该覆盖：

- 按业务、分页、时间或哈希桶拆。
- 查询模式决定拆分方式。
- 不缓存全部明细。
- 拆分后要处理删除和 TTL。
- 能结合购物车、评价、榜单举例。

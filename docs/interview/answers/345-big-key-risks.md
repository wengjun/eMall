# 345 大 key 有什么危害？

[返回按分类学习面试题](../README.md)

## 题目

大 key 有什么危害？

## 先给面试官的短答案

大 key 会导致 Redis 内存倾斜、网络传输变大、命令执行变慢、删除阻塞、复制压力增大、迁移困难，
严重时会拉高延迟甚至阻塞 Redis 主线程。

大 key 不只是 value 大，也包括集合元素数量过多。

## 大 key 类型

常见类型：

- 单个 String value 很大。
- Hash field 过多。
- List 元素过多。
- Set 成员过多。
- ZSet 成员过多。

集合型大 key 更隐蔽，因为单次操作可能扫描大量元素。

## 危害

危害包括：

- 读写耗时变长。
- 网络带宽被占满。
- 主线程被慢命令阻塞。
- 删除 key 阻塞。
- 主从复制延迟增加。
- Cluster slot 迁移变慢。
- 内存分布不均。

大 key 会放大故障影响。

## 发现方式

方式：

- Redis bigkeys 工具。
- 客户端统计 value 大小。
- 监控慢命令。
- 监控网络出口流量。
- 分片内存倾斜告警。
- 业务 key 前缀巡检。

大 key 要持续巡检，不能只在故障后排查。

## 在 eMall 项目中怎么讲？

eMall 购物车如果一个用户有大量 SKU，`cart:items:user` Hash 可能成为大 key。

商品评价列表如果全部放入一个 List，也会变成大 key。正确做法是分页、分片或只缓存摘要，
明细仍从数据库或搜索系统分页读取。

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
大 key 的危害是让 Redis 单次操作变重。它会增加内存倾斜、网络传输、命令执行时间、删除阻塞、
复制延迟和 slot 迁移成本。

生产中要限制 value 大小和集合元素数量，定期扫描 big key，并通过拆分、分页、摘要缓存和异步
删除治理。电商里购物车、评价列表、用户标签和排行榜都容易出现大 key。
```

## 回答评分点

高分答案应该覆盖：

- 大 key 包括大 value 和大集合。
- 会阻塞主线程和增加延迟。
- 影响复制和迁移。
- 需要监控和巡检。
- 能给出购物车、评价等场景。

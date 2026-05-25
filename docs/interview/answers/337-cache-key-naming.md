# 337 缓存 key 如何命名？

[返回按分类学习面试题](../README.md)

## 题目

缓存 key 如何命名？

## 先给面试官的短答案

缓存 key 命名要可读、可管理、可定位、可演进。常见格式是业务域、对象类型、版本、标识和字段，
例如 `product:detail:v1:sku:10001`。

好的 key 设计能降低冲突、方便排查、支持灰度和版本切换。

## 命名原则

原则：

- 使用统一分隔符。
- 包含业务域。
- 包含对象类型。
- 必要时包含版本号。
- 标识字段稳定。
- 避免过长 key。
- 避免用户输入直接拼接。

key 命名也是系统接口的一部分。

## 示例

示例：

```text
product:detail:v1:sku:10001
cart:item:v1:user:20001
coupon:claimed:v1:coupon:30001
rate-limit:api:v1:user:20001
flash-sale:token:v1:activity:90001
```

这些 key 能直接看出业务含义。

## 版本设计

版本号用于：

- 缓存结构升级。
- 灰度发布。
- 避免旧 value 反序列化失败。
- 快速切换 key 空间。

但版本切换会导致缓存重建，要控制雪崩风险。

## 在 eMall 项目中怎么讲？

eMall 可以约定 key 格式：

```text
{domain}:{object}:v{version}:{id-type}:{id}
```

例如商品详情用 `product:detail:v1:sku:10001`，购物车用 `cart:items:v1:user:20001`。
如果是 Redis Cluster 多 key 操作，可以使用 hash tag，例如 `cart:{user:20001}:items`。

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
缓存 key 要统一命名，常见做法是业务域、对象类型、版本和唯一标识组合。这样方便定位、统计、
删除和迁移。

生产中还要考虑版本演进、Redis Cluster hash tag、key 长度、用户输入安全和批量删除风险。
key 不是随手拼字符串，而是缓存体系的契约。
```

## 回答评分点

高分答案应该覆盖：

- key 要统一规范。
- 包含业务域和对象类型。
- 版本号支持演进。
- 避免冲突和过长。
- Redis Cluster 场景考虑 hash tag。

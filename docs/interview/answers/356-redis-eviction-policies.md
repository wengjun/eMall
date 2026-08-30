# 356 Redis 内存淘汰策略有哪些？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Redis 内存达到 `maxmemory` 后，会根据淘汰策略选择 key 删除。常见策略包括 noeviction、allkeys-lru、
volatile-lru、allkeys-lfu、volatile-lfu、allkeys-random、volatile-random 和 volatile-ttl。

策略选择要看 Redis 是否只做缓存，以及哪些 key 允许被淘汰。

## 策略分类

按范围分：

- allkeys：所有 key 都可能被淘汰。
- volatile：只有设置过期时间的 key 可能被淘汰。
- noeviction：不淘汰，写入报错。

按算法分：

- LRU：淘汰最近最少使用。
- LFU：淘汰访问频率低。
- random：随机淘汰。
- ttl：淘汰剩余 TTL 更短的 key。

## 如何选择

选择建议：

- 纯缓存场景可用 allkeys-lru 或 allkeys-lfu。
- 只有部分 key 可淘汰时用 volatile 策略。
- 不允许丢数据时用 noeviction。
- 热点明显时 LFU 可能更合适。

但更重要的是不要把 Redis 内存长期打满。

## 风险

风险包括：

- 淘汰关键 key 导致业务异常。
- 命中率下降导致数据库压力上升。
- 未设置 TTL 的 key 无法被 volatile 策略淘汰。
- 大 key 让内存倾斜更严重。

淘汰策略不是容量规划的替代品。

## 电商系统实践

大型电商系统商品详情缓存可以接受淘汰，但分布式锁、限流计数和关键短期状态不应和普通缓存混在同一个
Redis 实例中。

更好的做法是按用途拆分 Redis 集群，缓存集群使用合适淘汰策略，锁和限流集群严格控制内存和 TTL。

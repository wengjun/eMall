# 331 Redis Cluster 如何分片？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Redis Cluster 通过 16384 个 hash slot 做数据分片。每个 key 经过 CRC16 计算后对 16384 取模，
得到对应 slot，再由负责该 slot 的主节点处理请求。

Redis Cluster 的核心是 slot 归属，而不是简单按节点数量取模。

## 分片机制

流程：

- 客户端对 key 计算 CRC16。
- 对 16384 取模得到 slot。
- 根据 slot 路由到对应主节点。
- 主节点负责该 slot 的读写。
- 从节点复制主节点数据。
- slot 可以迁移到其他节点。

slot 让扩容迁移以 slot 为单位进行。

## hash tag

hash tag 用于让多个 key 落到同一个 slot。

例如：

```text
cart:{user123}:items
cart:{user123}:meta
```

只有 `{user123}` 参与 slot 计算。这样可以让同一用户购物车相关 key 在同一分片。

## 生产注意点

注意：

- 多 key 操作要求 key 在同一 slot。
- 热 key 仍可能打满单个分片。
- 扩容时要迁移 slot。
- 客户端要支持 MOVED 和 ASK 重定向。
- 不能把 Cluster 当强一致存储。

分片解决容量问题，不自动解决热点问题。

## 在 eMall 项目中怎么讲？

eMall 商品详情缓存可以按商品 ID 分散到不同 slot。购物车可以用 hash tag 让同一用户的多个 key
落在同一 slot，方便局部多 key 操作。

秒杀热点 SKU 不能只靠 Cluster，因为一个热点 key 仍然会落到一个 slot，需要做本地缓存、
热点复制或令牌化削峰。

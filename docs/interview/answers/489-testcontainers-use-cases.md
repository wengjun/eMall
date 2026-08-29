# 489 Testcontainers 适合验证什么？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Testcontainers 适合在测试中启动真实中间件容器，验证代码和 MySQL、PostgreSQL、Redis、Kafka、Elasticsearch
等依赖的真实协作。它特别适合验证 SQL、事务、索引、消息生产消费、序列化、缓存脚本和连接配置。

## 为什么需要 Testcontainers

传统集成测试常见问题是依赖本地环境，开发者机器和 CI 环境不一致。Testcontainers 把依赖封装成容器，
测试启动时动态创建，结束后销毁，可以让集成测试更可重复。

它比 mock 更真实，比共享测试环境更隔离，适合 CI 自动化。

## 适合验证的内容

数据库方面可以验证表结构、MyBatis 映射、事务回滚、唯一索引、乐观锁、分页查询和迁移脚本。

消息方面可以验证 Kafka topic、consumer group、消息序列化、重复消费、重试和死信逻辑。

缓存方面可以验证 Redis key、TTL、Lua 脚本、分布式锁和缓存穿透保护。

搜索方面可以验证索引 mapping、分词、查询条件和排序规则。

## 不适合什么

Testcontainers 不适合替代单元测试。它启动成本更高，运行速度更慢，也不应该用于覆盖所有业务分支。
复杂的跨服务端到端测试也不能只靠 Testcontainers，还需要服务编排、测试数据和环境治理。

## 在 eMall 项目中怎么讲？

eMall 可以用 Testcontainers 验证订单事务、库存防超卖、支付回调幂等、Outbox 事件、Kafka 消费、Redis 限流和
搜索索引查询。这样比纯 mock 更接近生产依赖行为。

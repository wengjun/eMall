# 318 数据库扩容如何做？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

数据库扩容要先明确瓶颈，再选择垂直扩容、读写分离、分库分表、归档、缓存或迁移到新集群。
生产扩容要做数据迁移、双写或增量同步、校验、灰度切流、回滚和监控。

扩容不是简单加机器，尤其是有状态数据库。

## 扩容方式

方式包括：

- 升级机器配置。
- 增加只读副本。
- 分库分表。
- 数据归档。
- 缓存热点读。
- 拆分业务库。
- 新老集群迁移。

先选择成本最低且风险可控的方式。

## 迁移流程

典型流程：

- 全量数据迁移。
- 记录分片或主键范围游标，分批执行全量迁移。
- 通过 CDC 或 binlog 位点同步增量变更。
- 数据校验。
- 影子读和灰度读流量。
- 在路由版本或写入栅栏保护下灰度写流量。
- 切换主流量。
- 保留回滚窗口。

切流不能一刀切。

迁移任务要持久化阶段、游标、源位点和校验结果，并保证每一步可重试、可暂停。目标端要通过业务版本或
更新时间拒绝旧增量覆盖新数据，校验不能只比较总行数，还要包含分段 checksum 和业务抽样。

## 风险点

风险包括：

- 数据不一致。
- 主从延迟。
- 双写失败。
- 路由错误。
- 回滚困难。
- 扩容期间性能下降。

所以扩容要演练。

## 在 eMall 项目中怎么讲？

订单库容量接近瓶颈时，可以先做历史归档和读写分离。

如果写入仍无法承载，再按分片键分库分表，并通过全量加增量同步和灰度路由迁移。

## 深度增强：扩容迁移图

![分库分表和数据库扩容迁移流程](../assets/sharding-expansion.svg)

数据库扩容最重要的是风险控制。对有状态核心库来说，真正难点不是“建一个新库”，
而是怎么迁移数据、同步增量、校验一致、灰度切流和保留回滚窗口。

## 深度增强：迁移任务模型

```java
public enum MigrationPhase {
    FULL_COPY,
    INCREMENTAL_SYNC,
    CHECKSUM_VERIFY,
    GRAY_READ,
    GRAY_WRITE,
    CUTOVER,
    ROLLBACK_WINDOW
}

public record MigrationTask(
        String taskId,
        String sourceCluster,
        String targetCluster,
        MigrationPhase phase,
        Instant startedAt,
        Instant updatedAt) {
}
```

迁移校验不能只看行数，还要抽样或分段 checksum：

```java
public record ChecksumRange(long startId, long endId, String sourceHash, String targetHash) {
}

public interface MigrationVerifier {

    List<ChecksumRange> verify(String tableName, long startId, long endId, long rangeSize);
}
```

## 深度增强：生产扩容步骤

1. 先定位瓶颈：CPU、IO、连接数、慢 SQL、锁等待、容量还是主从延迟。
2. 优先低风险方案：索引优化、归档、读写分离、缓存热点读。
3. 必须迁移时：全量复制历史数据。
4. 用 CDC、binlog 或双写同步增量。
5. 做行数、checksum 和业务抽样校验。
6. 灰度读流量，再灰度写流量。
7. 切主流量，并保留回滚窗口。

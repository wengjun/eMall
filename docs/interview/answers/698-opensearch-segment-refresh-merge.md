# 698 OpenSearch Segment、Refresh、Flush 和 Merge 如何工作？

[返回按分类学习面试题](../README.md)

## 从 shard 开始理解

一个 OpenSearch primary shard 本质上是一个 Lucene index，Lucene index 由多个不可变 segment 组成。
文档更新不是原地改旧 segment，而是写入新版本，并把旧版本标记删除；真正释放空间要等 merge 重写 segment。

```text
写请求 -> indexing buffer + translog
             | refresh
             v
         新可搜索 segment
             | merge
             v
        更少、更大的 segment
```

## Refresh：变得可搜索，不等于完整持久化

Refresh 把内存中的索引结构发布为新的 Lucene segment，并打开新的 searcher，因此文档从近实时状态变成可搜索。
频繁 refresh 可降低写后搜索延迟，但会制造大量小 segment、增加文件句柄、搜索 fan-out 和后续 merge 成本。

写 API 返回成功后，按 `_id` 的实时 GET 与普通 search 的可见时点可能不同。需要“写后立即搜索”时，
可以使用适当的 refresh 策略，但不能让每条大促写入都强制 refresh；更合理的是业务读己之写走主数据或按批等待。

## Flush：建立新的恢复基线

Flush 执行 Lucene commit 并滚动 translog generation，使较早 translog 可以删除。
它主要控制恢复成本和日志生命周期，不是让文档可搜索的动作。OpenSearch 会按阈值自动处理，
业务通常不应每次写后手工 flush。

translog 帮助节点崩溃后重放尚未进入安全 Lucene commit 的操作；副本机制和 translog durability 共同决定确认写入的恢复边界。

## Merge：后台写放大的来源

Merge 选择若干 segment，生成一个新 segment，并丢弃已删除文档，然后删除旧文件。收益是减少搜索要访问的 segment、回收删除空间；成本是大量顺序读写、CPU 和临时磁盘占用。

大批更新会同时产生：新文档写入、旧文档 tombstone、后台 merge 三份压力。磁盘水位过高时 merge 空间不足，索引甚至会进入只读保护。

`force_merge` 只适合已经停止写入的只读索引。对仍在更新的索引强制生成超大 segment，后续自动 merge 难以处理，反而长期保留删除文档和放大快照成本。

## eMall 调优方法

- 商品批量导入时适当增大 refresh interval，完成后恢复并验证搜索可见性。
- 按时间滚动索引，把不再写入的历史索引设为只读后再考虑 force merge。
- 同时监控 indexing rate、refresh/merge time、segment 数、deleted docs、translog 大小、磁盘水位和 JVM 压力。
- 容量预留必须覆盖 merge 临时空间与副本恢复，不能按最终文档净大小配盘。

参考：[OpenSearch Force Merge](https://docs.opensearch.org/latest/api-reference/index-apis/force-merge/)

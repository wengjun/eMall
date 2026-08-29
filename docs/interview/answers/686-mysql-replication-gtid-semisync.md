# 686 binlog 复制、GTID 和半同步复制如何工作？

[返回按分类学习面试题](../README.md)

## 异步复制的数据流

```text
Source 提交 -> binlog
                    -> Replica receiver -> relay log -> applier worker -> InnoDB
```

Source 写完本地事务即可返回；Replica 的 receiver 拉取事件并写 relay log，applier 再重放。网络延迟、单热点键冲突、大事务、Replica IO 或 SQL 执行慢都可能造成复制延迟。

“Replica 已收到”与“Replica 已应用并可读”是两个不同位点。只监控网络接收延迟会漏掉 applier 堵塞。

## GTID 的作用和边界

GTID 为复制拓扑中的已提交事务提供全局唯一标识，形式上包含源标识和事务序号。每个实例维护已执行 GTID 集合，连接新 Source 时可自动请求自己缺失的事务，不再依赖人工计算 binlog 文件名和 position。

GTID 简化切换与去重判断，但不会自动选出最安全的新主，也不保证副本没有业务级冲突。候选者必须包含所有要求保留的已提交 GTID；从多个分叉节点拼接历史仍需要运维协议。

## 半同步到底等待到哪里

半同步让 Source 在返回提交前等待至少一个指定数量的 Replica 确认已收到并记录 binlog 事件。它通常**不等待 Replica 应用事务**，所以切换后立即查询仍可能短暂看不到数据。

当确认超时后，具体配置可能退化到异步复制以维持写可用性。此时监控必须明确显示半同步已失效，否则团队误以为 RPO 仍为零。

## 并行复制为何仍会落后

Replica 可以并行应用无冲突事务，但以下因素限制并行度：

- 单个超大事务必须作为一个长任务处理。
- 大量事务更新同一库存行或同一依赖序列。
- Replica 硬件、索引或参数弱于 Source。
- DDL、锁等待和下游备份争抢 IO。

优化不能只增加 worker；应拆小事务、减少热点、校准磁盘能力并观察各 worker 队列。

## eMall 中的读写策略

下单成功后的立即查询不能随机打到落后 Replica。可在会话中短暂读 Source，或携带提交 GTID/位点，只有副本追到该位置才放行。商品列表等允许陈旧的读可以直接使用 Replica。

故障切换演练要记录最后确认 GTID、各候选 executed set、半同步退化区间和未决幂等请求，才能给出真实 RPO。

参考：

- [MySQL 8.4 GTID](https://dev.mysql.com/doc/refman/8.4/en/replication-gtids-concepts.html)
- [MySQL 8.4 Semisynchronous Replication](https://dev.mysql.com/doc/refman/8.4/en/replication-semisync.html)

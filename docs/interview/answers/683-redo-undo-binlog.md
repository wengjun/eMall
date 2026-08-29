# 683 redo log、undo log 和 binlog 分别解决什么问题？

[返回按分类学习面试题](../README.md)

## 三类日志的职责边界

| 日志 | 所属层 | 核心用途 | 典型读取者 |
| --- | --- | --- | --- |
| redo log | InnoDB | WAL 与崩溃后前滚，保证已提交修改可恢复 | InnoDB 恢复线程 |
| undo log | InnoDB | 事务回滚与 MVCC 历史版本 | 回滚、快照读、purge |
| binlog | MySQL Server | 复制、CDC、时间点恢复 | Replica、Debezium、mysqlbinlog |

把它们都叫“恢复日志”会掩盖完全不同的生命周期和一致性问题。

## redo：先记变化，再延迟刷数据页

InnoDB 修改 Buffer Pool 中的数据页，同时生成带 LSN 的 redo record。事务提交要求相应 redo 按配置持久化；
数据页不必在提交时落盘。崩溃恢复从 checkpoint 附近重放 redo，使磁盘上的旧页前滚到应有状态。

redo 是有限循环空间。checkpoint 推进后，已经不再需要的前缀才能复用；脏页刷盘落后会扩大 checkpoint age，最终反压前台写入。

## undo：不是备份，也不是简单反向 SQL

更新记录前，InnoDB 保存构造旧版本所需的信息，并通过隐藏事务标识和回滚指针形成版本链。它用于：

- 当前事务失败时撤销尚未提交修改。
- 一致性读按 Read View 找到对自己可见的历史版本。
- purge 在不再有活跃快照需要旧版本后清理历史。

长事务会长期阻止 purge，导致 history list、undo 表空间和二级索引清理压力增长。因此“只读事务不写数据”也可能给系统造成严重负担。

## binlog：跨引擎的逻辑变更事实

生产通常使用 row format，记录行事件而不是让 Replica 重新执行可能不确定的 SQL。binlog 是复制和 CDC 的来源，
也可在恢复物理备份后重放到目标时间点。它不能替代 InnoDB redo，因为恢复粒度、写入层次和性能目标不同。

## 为什么提交需要协调两份持久化日志

若 redo 已提交但 binlog 丢失，主库有数据而 Replica/CDC 永远看不到；若 binlog 有事件但引擎回滚，
Replica 会产生主库不存在的数据。MySQL 用内部两阶段提交把 InnoDB redo 状态与 binlog 是否存在关联起来，
恢复时据此决定 prepared 事务提交还是回滚。

```text
数据修改 -> undo + redo
          -> redo prepare
          -> binlog 持久化
          -> InnoDB commit
```

## eMall 排障抓手

订单写入后搜索没有更新，应先区分：事务是否提交、binlog 是否生成、CDC 位点是否推进、消费者是否幂等处理，而不是笼统说“数据库日志有问题”。长事务导致 undo 膨胀时，应定位快照持有者而非直接删除 undo 文件。

参考：

- [MySQL 8.4 Redo Log](https://dev.mysql.com/doc/refman/8.4/en/innodb-redo-log.html)
- [MySQL 8.4 Undo Logs](https://dev.mysql.com/doc/refman/8.4/en/innodb-undo-logs.html)

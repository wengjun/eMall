# 684 MySQL 一次事务提交经历什么过程？

[返回按分类学习面试题](../README.md)

## 从 SQL 到提交

以 InnoDB、开启 binlog 的写事务为例：

1. Server 层解析、优化并调用存储引擎执行计划。
2. InnoDB 获取必要锁，修改 Buffer Pool，写 undo 并生成 redo；此时数据页通常尚未刷盘。
3. `COMMIT` 时 InnoDB 把事务写到 redo prepare 状态。
4. Server 层写入并按配置同步 binlog。
5. InnoDB 把事务标记为 commit，释放锁并向客户端返回。

prepare 与 binlog 之间的协调保证崩溃后能够根据 binlog 判断 prepared 事务应提交还是回滚。

## Group Commit 为什么重要

每个事务单独 `fsync` 会让吞吐受存储 IOPS 限制。Group Commit 把一批并发事务在 flush、sync、commit 阶段合并，
让一次同步操作服务多个事务，同时保持 binlog 顺序与 InnoDB 提交顺序可协调。

```text
T1 --\
T2 ----> 写入批次 -> 一次 fsync -> 分别完成提交
T3 --/
```

吞吐提高并不意味着单事务不需要等待持久化；它利用的是并发批处理。

## 两个关键耐久参数

- `innodb_flush_log_at_trx_commit=1` 通常表示每次事务提交都把 redo 写入并同步到持久介质。
- `sync_binlog=1` 通常表示每个 binlog 提交组同步 binlog。

降低它们可减少同步 IO，但会把故障时可能丢失的已返回事务窗口扩大。云盘写缓存、虚拟化和 RAID 控制器是否真正遵守 flush 语义同样重要，参数为 1 不是端到端耐久性的全部证明。

## Java 事务边界如何映射

```java
@Transactional
public OrderId createOrder(CreateOrder command) {
    orderMapper.insert(toOrder(command));
    outboxMapper.insert(toEvent(command));
    return command.orderId();
}
```

Spring 代理在方法正常返回时调用连接提交，抛出符合回滚规则的异常时回滚。以下做法会破坏预期：同类自调用绕过代理、在事务中做长时间 RPC、捕获异常后不再抛出、异步线程继续使用原事务语义。

事务中插入 outbox 能保证业务行和待发布事件同库提交，但不保证消息已经发送；发送由后续发布器完成并需要幂等。

## 客户端超时不是回滚证明

提交完成但响应在网络中丢失时，客户端看到超时，数据库可能已经提交。下单 API 必须用业务请求号或幂等键查询结果并以相同键重试，不能把超时直接解释为“订单未创建”。

排障时应同时检查锁等待、redo/fsync 延迟、binlog group commit、事务提交速率和连接池占用，才能判断慢在执行阶段还是持久化阶段。

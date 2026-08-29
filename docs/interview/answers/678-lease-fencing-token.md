# 678 租约和 fencing token 为什么比单纯分布式锁更安全？

[返回按分类学习面试题](../README.md)

## 单纯锁的致命窗口

带 TTL 的锁只能说明“协调服务目前认为谁持有锁”，不能撤销旧持有者已经获得的执行能力。

```text
工作者 A 获取锁 token=41
A 因长 GC 暂停，租约过期
工作者 B 获取锁 token=42 并写入新结果
A 恢复，误以为自己仍是持有者并覆盖 B
```

即使 A 在写前再次查询锁，也存在“查询通过后、真正写入前再次暂停”的竞态。删除锁时比较随机 owner 值只能防止误删 B 的锁，不能防止 A 写下游资源。

## 租约解决活性，fencing token 解决安全

- 租约让失联持有者最终失去资格，避免资源永久卡死。
- fencing token 是每次成功获得租约时产生的严格单调序号。
- 真正被保护的存储或设备必须记住已接受的最大 token，并拒绝更小 token 的请求。

数据库条件更新示例：

```sql
UPDATE settlement_task
SET result = ?, fence_token = ?
WHERE task_id = ? AND fence_token < ?;
```

只有影响一行才代表写入被接受。A 携带 41 恢复时，目标记录已是 42，因此无法覆盖 B。

## Java 侧应把令牌做成显式协议字段

```java
public record Lease(long fencingToken, Instant expiresAt) {}

public void persistResult(long taskId, Lease lease, String result) {
    int changed = mapper.updateIfTokenIsNewer(
            taskId, lease.fencingToken(), result);
    if (changed != 1) {
        throw new StaleLeaseException(taskId, lease.fencingToken());
    }
}
```

不能只在进程内调用 `lease.expiresAt()` 判断，因为本地时钟漂移和停顿仍会造成窗口；最终裁决必须发生在资源端的原子写入中。

## token 从哪里来

可使用共识日志索引、数据库序列或支持线性一致自增的协调服务。普通时间戳不保证严格单调，随机 UUID 只能唯一而不能比较先后。若使用 Redis `INCR`，还必须说明故障切换后计数器是否可能回退以及持久化级别。

当下游是无法验证 token 的外部 HTTP API 或物理设备时，fencing 无法凭空成立。可以改为单写网关、把命令先写入有序日志，或让下游支持幂等版本条件。

## 在 eMall 中的落点

结算任务、补偿任务和批量对账只能有一个执行者时，协调层发放租约和 token；任务表用 token 条件更新保护最终结果。
还应监控租约续期失败、过期后仍执行、stale token 拒绝次数，并用“暂停旧持有者超过 TTL”的故障注入测试证明安全性。

# 347 Redis 分布式锁如何实现？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Redis 分布式锁通常使用 `SET key value NX PX ttl` 实现加锁，value 使用唯一请求标识。释放锁时
用 Lua 脚本先比较 value 是否一致，再删除 key，避免误删别人的锁。

锁必须设置过期时间，并且业务执行时间要小于锁 TTL 或具备续期机制。

## 加锁

加锁命令：

```text
SET lock:order:10001 request-uuid NX PX 3000
```

含义：

- NX 表示 key 不存在才设置。
- PX 表示毫秒级过期时间。
- value 用来标识锁持有者。

只有返回成功才算拿到锁。

## 解锁

解锁要用 Lua：

```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

原因是比较 value 和删除必须原子执行。

## 风险

风险包括：

- 锁 TTL 过短导致业务未完成锁已过期。
- 业务卡住导致锁被长时间持有。
- 误删其他线程锁。
- Redis 主从切换导致锁状态丢失。
- 锁粒度过粗影响吞吐。

分布式锁不能替代数据库约束和幂等。

## 在 eMall 项目中怎么讲？

eMall 可以在防重复提交、低频后台任务和活动配置更新中使用 Redis 锁。

但扣库存不能只依赖 Redis 锁，仍要用数据库条件更新或库存服务原子扣减保证不超卖。Redis 锁用于
降低并发冲突，不作为最终一致性证明。

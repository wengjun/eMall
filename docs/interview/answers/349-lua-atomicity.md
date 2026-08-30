# 349 Lua 脚本为什么能保证原子性？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Redis 执行 Lua 脚本时，会把脚本作为一个整体在 Redis 执行线程中运行。脚本执行期间不会插入执行
其他客户端命令，因此脚本内部多个 Redis 操作对其他客户端表现为原子。

它保证的是 Redis 单实例内的脚本执行原子性，不等于跨系统事务原子性。

## 为什么原子

原因：

- Redis 命令执行模型是串行的。
- Lua 脚本作为一个命令执行。
- 脚本执行期间不会被其他命令打断。
- 脚本内读写的结果对外一次性生效。

所以比较锁 value 和删除锁可以放在一个脚本里。

## 示例

下面用“检查并扣减”说明多条 Redis 命令如何组成一个原子操作：

```lua
local available = tonumber(redis.call("get", KEYS[1]) or "-1")
local quantity = tonumber(ARGV[1])
if available < quantity then
    return 0
end
redis.call("decrby", KEYS[1], quantity)
return 1
```

如果不用 Lua，读取库存和扣减之间可能插入其他客户端命令，多个调用方就可能都基于同一个旧值通过校验。

## 边界

边界包括：

- 长 Lua 会阻塞 Redis。
- 脚本不能做耗时计算。
- Cluster 下多 key 要在同一 slot。
- Redis 故障不能保证跨实例事务。
- Lua 不能替代业务幂等。

Lua 要短小、确定、可控。

## 电商系统实践

大型电商系统可以用 Lua 实现 Redis 锁释放、限流计数和令牌扣减。

例如秒杀令牌扣减时，Lua 可以检查令牌数量并扣减，避免先读后写之间被并发打断。但最终订单创建
仍要依靠幂等和库存扣减确认。

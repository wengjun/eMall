# 350 Redisson 看门狗解决什么问题？

[返回按分类学习面试题](../README.md)

## Java 调用方式决定租约模式

不指定 leaseTime 获取 RLock 时，Redisson 可以通过 watchdog 自动续期；
显式指定 leaseTime 时，锁通常在该固定租约结束后释放，不再由 watchdog 持续续期。
默认 watchdog 超时为 30 秒，可配置；不要把默认值当业务执行上限。

以下是方法片段，redisson 为复用的 RedissonClient，方法允许抛 InterruptedException：

```java
RLock lock = redisson.getLock("order:42");
if (!lock.tryLock(200, TimeUnit.MILLISECONDS)) {
    throw new IllegalStateException("lock not acquired");
}
try {
    updateOrder();
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

这里 200 ms 是等待获取锁的时间，不是持有锁的租约。带 waitTime、leaseTime、unit 三个参数的
重载含义不同，见 [Redisson 锁文档](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/)。

## 续期不等于无限安全

长时间 GC 停顿、进程失联、Redis 故障都可能导致续期失败。
本地代码不一定立刻停下，所以 watchdog **不能保证失去锁的旧持有者不再写入**。
isHeldByCurrentThread 也不是业务写入的原子保护，检查后状态仍可能变化。

普通 RLock 有线程所有权，不能在任意异步回调线程随意 unlock。
数据库版本校验或 fencing 属于写入侧约束，本题不重复分布式锁协议设计。

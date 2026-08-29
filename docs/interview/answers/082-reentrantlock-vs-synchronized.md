# 082 ReentrantLock 和 synchronized 怎么选？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

简单同步优先用 `synchronized`，因为语法简单、自动释放、JVM 优化充分。需要可中断获取锁、超时尝试、
公平锁、多条件队列或更灵活控制时，选择 `ReentrantLock`。

选择标准不是哪个更高级，而是业务是否需要 `ReentrantLock` 的额外能力。

## synchronized 的特点

优点：

- 语法简单。
- 自动释放锁。
- 异常时不容易忘记解锁。
- JVM 内置优化。
- 可读性好。

缺点：

- 不能尝试加锁后立即返回。
- 不能设置加锁超时。
- 不能响应中断等待。
- 条件队列能力较弱。

适合简单临界区。

## ReentrantLock 的特点

`ReentrantLock` 是显式锁。

优点：

- `tryLock()`。
- `tryLock(timeout)`。
- `lockInterruptibly()`。
- 可选择公平锁。
- 支持多个 `Condition`。
- 可查询锁状态。

缺点：

- 必须手动释放锁。
- 忘记 `unlock()` 会导致严重故障。
- 代码更复杂。

正确写法：

```java
lock.lock();
try {
    update();
} finally {
    lock.unlock();
}
```

## 什么场景用 synchronized？

适合：

- 临界区很短。
- 锁逻辑简单。
- 不需要超时。
- 不需要中断等待。
- 不需要多个条件队列。

例如保护本地计数器、小缓存元数据、简单状态切换。

## 什么场景用 ReentrantLock？

适合：

- 获取不到锁时要快速失败。
- 等待锁需要超时。
- 等待锁时要支持中断。
- 需要公平锁。
- 需要多个条件队列。
- 需要更复杂同步控制。

例如库存热点保护中，请求等待锁超过 20 ms 就降级或返回重试提示。

## 公平锁选择

`ReentrantLock` 可以创建公平锁：

```java
new ReentrantLock(true)
```

公平锁减少插队，但吞吐通常更低。

默认非公平锁吞吐更好，适合大多数高并发服务。

只有在强公平要求或饥饿风险明显时才考虑公平锁。

## Condition 的价值

`Condition` 可以创建多个等待队列。

相比 `Object.wait/notify`，它更适合复杂同步场景。

例如一个阻塞队列可以分别有：

- notEmpty 条件。
- notFull 条件。

生产业务通常优先使用成熟并发工具，而不是自己写复杂条件同步。

## 在 eMall 项目中怎么讲？

eMall 中简单本地状态保护可以用 `synchronized`。

如果秒杀热点商品需要获取锁失败后快速降级，可以用 `ReentrantLock.tryLock(timeout)`，避免请求无限等待。

如果是跨实例库存一致性，单机锁都不够，要使用数据库条件更新、Redis 原子操作或消息串行化。

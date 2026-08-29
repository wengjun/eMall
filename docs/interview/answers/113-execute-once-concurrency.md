# 113 并发下如何实现只执行一次？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

只执行一次要先明确范围：单 JVM、单数据库资源还是分布式多实例。单 JVM 可以用原子变量、锁或 `ConcurrentHashMap`；
分布式场景通常用数据库唯一键、幂等表、状态机、消息去重或分布式锁加资源端约束。

真正可靠的做法是让“执行结果”可幂等，而不是只依赖“执行过程”只发生一次。

## 单 JVM 只执行一次

可以用 `AtomicBoolean`：

```java
if (started.compareAndSet(false, true)) {
    startJob();
}
```

这只在当前进程有效。

多实例部署时不够。

## 数据库唯一键

分布式防重复常用唯一键。

例如：

```sql
insert into idempotent_record(request_id, status) values (?, 'PROCESSING')
```

`request_id` 唯一。

只有插入成功的实例执行，插入失败的实例查询已有结果。

## 状态机

状态机通过状态条件控制只执行一次。

例如订单支付：

```sql
update orders
set status = 'PAID'
where order_id = ? and status = 'CREATED'
```

只有一个请求能把 `CREATED` 改成 `PAID`。

后续重复请求发现状态已变更，返回幂等结果。

## 消息消费只执行一次

消息队列通常提供至少一次投递。

消费者要自己做幂等：

- 消费记录表。
- 业务唯一键。
- 状态机条件更新。
- 去重 key。

不要假设 MQ 永远只投递一次。

## 分布式锁的角色

分布式锁可以减少并发进入，但不能作为唯一保障。

因为锁可能失效。

更稳妥：

```text
分布式锁降低冲突 + 数据库唯一键兜底
```

## 在 eMall 项目中怎么讲？

支付回调只处理一次：

- `payment_no` 建唯一键。
- 订单状态从 `UNPAID` 条件更新为 `PAID`。
- 重复回调查询已支付结果并返回成功。
- 发送消息使用 outbox 防重复。

这样即使并发回调，也不会重复改订单或重复发货。

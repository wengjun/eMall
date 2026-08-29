# 691 MyBatis 一级、二级缓存的事务边界和风险是什么？

[返回按分类学习面试题](../README.md)

## 一级缓存不是全局缓存

一级缓存属于 `SqlSession`，默认作用域是 session。同一 Session 中，statement、参数、分页边界等组成相同 cache key 的查询可能直接返回缓存结果；执行写操作、提交、回滚或关闭等动作会按规则清理。

在 Spring 中，事务范围内通常复用绑定到当前线程的 Session；无事务的独立 Mapper 调用可能各自获得不同 Session。因此不能仅凭“连续调用了两次 Mapper”推断一定命中一级缓存。

## 一级缓存的隐蔽风险

```java
@Transactional
public void example(long orderId) {
    Order first = orderMapper.selectById(orderId);
    externalJdbcClient.updateOrder(orderId); // MyBatis does not know this write.
    Order second = orderMapper.selectById(orderId); // May reuse the local value.
}
```

同一事务通过另一个 namespace、原生 JDBC、存储过程或外部系统修改数据时，当前 Session 未必知道相关缓存应失效。
把 `localCacheScope` 改为 `STATEMENT` 可缩小范围，但要根据延迟加载等行为回归测试。

## 二级缓存的真实边界

二级缓存按 Mapper namespace 共享，可跨 Session 使用；通常在事务成功提交后，暂存的缓存变更才对其他 Session 可见。namespace 内写操作会让该 namespace 的缓存失效。

问题在于业务聚合常跨多个 Mapper：`OrderMapper` 更新一张表，`OrderViewMapper` 的 join 查询也依赖该表，
但两个 namespace 不会自动建立依赖关系。数据库外部写入、CDC 回写和多实例部署会进一步扩大陈旧窗口。

## 对象可变性问题

若缓存实现返回共享的可变对象引用，调用方修改 DTO 可能污染后续读取。序列化型缓存能隔离引用，却增加 CPU、内存与兼容性成本。无论哪种方式，都应把持久化实体当作数据快照，不在查询后随意修改并期待数据库同步。

## 为什么生产更常用显式缓存

Redis/Caffeine 等显式缓存能表达 cache key、TTL、版本、失效事件、命中率和降级策略；
MyBatis 二级缓存藏在数据访问层，难以表达跨聚合依赖和业务陈旧预算。
高一致交易查询通常关闭二级缓存，一级缓存也保持短事务边界。

## 测试必须覆盖事务可见性

- 同一 Session 重复读是否命中，写后是否失效。
- 两个 Session 并发读写时何时可见。
- 事务回滚后是否把未提交结果泄漏到共享缓存。
- namespace A 更新后，依赖它的 namespace B 查询是否陈旧。
- 多实例与数据库外部写入后的行为。

在 eMall 中，订单状态、库存和支付记录不依赖 MyBatis 二级缓存；商品详情缓存由业务层显式维护版本与失效事件，缓存故障时仍能回源数据库并保持正确性。

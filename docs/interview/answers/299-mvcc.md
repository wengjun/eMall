# 299 MVCC 是什么？

[返回按分类学习面试题](../README.md)

## 题目

MVCC 是什么？

## 先给面试官的短答案

MVCC 是多版本并发控制，通过为数据保留多个版本，让读操作可以读一致性快照，而不必阻塞写操作。
InnoDB 通过隐藏事务字段、undo log 和 Read View 实现 MVCC。

它提升并发读写性能，是快照读的基础。

## 为什么需要 MVCC？

如果读写都靠锁：

- 读会阻塞写。
- 写会阻塞读。
- 并发性能差。

MVCC 让普通一致性读读取历史版本，从而减少读写冲突。

## InnoDB 如何实现？

关键组件：

- 隐藏事务 ID。
- 回滚指针。
- undo log。
- Read View。

Read View 决定当前事务能看到哪些版本。

## MVCC 能解决什么？

它能支持：

- 快照读。
- 一致性读。
- 减少读写阻塞。
- 实现部分隔离级别语义。

但当前读、加锁读和更新仍会涉及锁。

## 在 eMall 项目中怎么讲？

订单列表查询可以使用普通快照读，不阻塞订单状态更新。

但库存扣减必须使用当前读或条件更新，不能只依赖快照读判断库存。

## 深度增强：数据访问和扩展图

![数据库、缓存和消息一致性链路](../assets/data-cache-mq.svg)

数据库题要从访问路径、索引、锁、事务和容量出发。电商系统的数据层既要支撑高并发读写，
又要保证订单、库存、支付等事实数据可追踪。缓存和消息可以提升性能，但不能替代数据库事实来源。

## 深度增强：Java 17 数据访问策略示例

```java
record QueryPlan(String accessPath, boolean usesIndex, boolean requiresPagination) {

    boolean safeForOnlineTraffic() {
        return usesIndex && requiresPagination;
    }
}

final class OnlineQueryPolicy {

    void verify(QueryPlan plan) {
        if (!plan.safeForOnlineTraffic()) {
            throw new IllegalArgumentException("Online query must use index and pagination");
        }
    }
}
```

这段代码体现线上查询治理：不是 SQL 能跑就可以上线，而是要确认走索引、可分页、可限流、可观测。

## 深度增强：生产边界

核心表设计要从典型查询倒推索引，避免全表扫描、深分页和大事务。分库分表要先选好分片键，
避免跨分片事务和热点分片。任何数据迁移都要支持灰度、校验、回滚或修复。

## 深度增强：面试高分表达

我会从访问模式回答数据题：谁查、按什么条件查、QPS 多少、数据量多大、是否强一致、是否需要分页和排序。
然后再决定索引、分片、缓存、读写分离和归档策略。

## 专家级完整回答

```text
MVCC 是多版本并发控制。数据库为数据维护多个版本，读事务根据 Read View 读取可见版本，
从而让普通读不阻塞写、写也不阻塞普通读。

InnoDB 依赖隐藏事务 ID、回滚指针和 undo log 构造历史版本。但更新和 select for update 属于当前读，仍需要锁。
```

## 回答评分点

高分答案应该覆盖：

- MVCC 是多版本并发控制。
- 目标是减少读写阻塞。
- InnoDB 依赖 undo log 和 Read View。
- 快照读使用 MVCC。
- 当前读仍涉及锁。

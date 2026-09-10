# 446 HikariCP 连接池耗尽时应检查哪些 Java 配置？

[返回按分类学习面试题](../README.md)

## 参数不是同一种超时

| HikariCP 属性 | 含义 |
| --- | --- |
| maximumPoolSize | 池中总连接上限，不只是活跃连接 |
| minimumIdle | 尽量维持的空闲连接数 |
| connectionTimeout | 从池获取连接的等待上限，不是 SQL 超时 |
| maxLifetime | 连接生命周期管理，不会强行关闭正在借用的连接 |
| leakDetectionThreshold | 长时间借用的诊断阈值，不是自动回收机制 |

属性契约见 [HikariCP 配置说明](https://github.com/brettwooldridge/HikariCP)。
不能用降低 maxLifetime 治疗一个永远不返回的 SQL，也不能因为没看到泄漏日志就断言不存在长事务。

## MyBatis 与 Spring 的连接归还

MyBatis-Spring 通常随 Spring 事务管理 SqlSession 和连接。Mapper 方法返回不等于外层事务结束：
若 @Transactional 方法后面还在执行远程调用，连接仍可能被持有。

原生自行获得的 Connection 必须按生命周期关闭；通过 Spring 管理的会话则不能随意手动提交、回滚或关闭。
`Connection.close()` 对池代理通常意味着归还连接，并不每次物理断开 TCP。

## 用 Java 证据定位

线程 dump 出现大量 HikariPool.getConnection 等待时，先找已经借走连接的线程，
结合事务栈、Hikari active/idle/pending 和泄漏诊断中的借用位置。
REQUIRES_NEW 可能在外层持有连接时再借一条，见 [125](125-required-requires-new-nested.md)。
不再重复慢 SQL 优化和数据库扩容方案。

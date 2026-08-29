# 690 MyBatis 从 Mapper 调用到 SQL 执行经历什么过程？

[返回按分类学习面试题](../README.md)

## 先建立对象关系

MyBatis 的 Mapper 接口没有手写实现类。启动时它把 XML/注解解析为 `MappedStatement`，运行时为接口创建 JDK 动态代理。一次调用的主链路是：

```text
MapperProxy
  -> MapperMethod
  -> SqlSession
  -> Executor
  -> StatementHandler
  -> ParameterHandler / TypeHandler
  -> JDBC PreparedStatement
  -> ResultSetHandler / ObjectFactory
```

理解这条链路后，才能判断缓存、插件、事务和慢 SQL 分别位于哪里。

## 启动阶段发生什么

`SqlSessionFactory` 构建时，`Configuration` 收集：

- `MappedStatement`：SQL 源、命令类型、参数映射、结果映射、缓存配置等。
- `ResultMap`、`ParameterMap`、`TypeHandler`、语言驱动和插件。
- Mapper 接口到 `MapperProxyFactory` 的注册关系。

statement id 通常是“接口全限定名 + 方法名”。重载 Mapper 方法容易产生映射歧义，因此不应像普通 Java API 那样随意重载。

## 一次查询如何执行

1. `MapperProxy` 缓存方法解析结果，并把参数封装后交给 `MapperMethod`。
2. `MapperMethod` 根据 `SqlCommand` 调用 `SqlSession.selectOne/selectList/update` 等。
3. `SqlSession` 找到 `MappedStatement`，调用配置的 `Executor`。
4. `Executor` 处理一级缓存、延迟加载和批处理策略，再创建 `StatementHandler`。
5. 动态 SQL 生成最终 `BoundSql`；`ParameterHandler` 通过 `TypeHandler` 绑定 `?`。
6. JDBC Driver 发送请求；`ResultSetHandler` 按 `ResultMap` 创建 Java 对象并填充属性。

`SimpleExecutor` 每次创建 Statement，`ReuseExecutor` 尝试复用，`BatchExecutor` 批量提交；开启二级缓存时外层还会使用 `CachingExecutor` 装饰实际 Executor。

## Spring 集成改变了什么

`SqlSessionTemplate` 是线程安全门面，但底层真实 `SqlSession` 不是线程安全对象。Spring 事务期间，
它从事务同步上下文获取同一个 Session/Connection；事务结束后提交或回滚并关闭资源。
不要把 Mapper 调用放入自己新建的线程后期待继承原事务。

```java
@Transactional
public void reserve(Reservation reservation) {
    int changed = inventoryMapper.reserve(
            reservation.skuId(), reservation.quantity());
    if (changed != 1) {
        throw new InsufficientInventoryException(reservation.skuId());
    }
}
```

这段代码的正确性来自 SQL 条件更新和事务，而不是 Mapper 代理本身。

## SQL 安全与性能边界

`#{value}` 生成参数占位符并通过 TypeHandler 绑定；`${value}` 做字符串替换，只有经过白名单验证的表名、列名等结构片段才可使用。用户输入直接进入 `${}` 会造成 SQL 注入。

排查慢调用时，应分开观察动态 SQL 构造、连接池等待、数据库执行、结果集传输和对象映射。返回十万行时，即使数据库执行只需 20 ms，ResultSet 映射和 JVM 分配仍可能成为主要成本。

参考：[MyBatis Executor API](https://mybatis.org/mybatis-3/apidocs/org/apache/ibatis/executor/Executor.html)

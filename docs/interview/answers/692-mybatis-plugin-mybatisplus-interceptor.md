# 692 MyBatis 插件和 MyBatis-Plus 拦截器如何工作？

[返回按分类学习面试题](../README.md)

## MyBatis 插件的本质

插件用 JDK 动态代理包裹 MyBatis 的四类扩展对象：`Executor`、`StatementHandler`、`ParameterHandler`
和 `ResultSetHandler`。`@Intercepts` 声明要拦截的方法签名，`Plugin.wrap` 只在目标接口匹配时创建代理。

```java
@Intercepts(@Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}))
public final class WriteAuditInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        try {
            return invocation.proceed();
        } finally {
            AuditContext.record(statement.getId());
        }
    }
}
```

插件链是多层代理，注册顺序会影响嵌套顺序。修改 SQL、分页、租户条件和审计若顺序错误，可能出现分页总数未加租户条件或安全检查检查了改写前 SQL。

## MyBatis-Plus 的两层关系

MyBatis-Plus 仍建立在 MyBatis 插件点上。常用 `MybatisPlusInterceptor` 作为外层拦截器，内部按顺序运行多个 `InnerInterceptor`，例如分页、多租户、乐观锁、防全表更新和动态表名。

它减少通用 CRUD 与 SQL 改写代码，但不会改变数据库事务、锁和索引原理。`updateById` 也可能产生丢失更新，必须使用 `@Version` 或业务条件；逻辑删除也不能替代唯一约束和数据归档策略。

## 多租户改写的安全边界

自动追加 `tenant_id` 前必须保证：

- 所有受控表都被识别，join、子查询、CTE 和批量 SQL 有测试。
- 租户 ID 来自已认证上下文，不接受请求体自报。
- 异步线程和定时任务显式传递上下文，不能依赖泄漏的 `ThreadLocal`。
- 数据库唯一索引包含租户维度，应用条件不是最后一道隔离线。

SQL Parser 对复杂语句有 CPU 成本，动态表名还会影响 statement cache 和观测聚合。

## 分页与乐观锁常见坑

- 深分页即使用插件生成 `LIMIT offset,size`，数据库仍可能扫描并丢弃大量行；核心列表应使用 seek pagination。
- 乐观锁更新后必须检查影响行数，0 行意味着冲突或记录不存在，不能静默返回成功。
- 批处理需要关注 Executor 类型、flush 时机、单批大小和失败后的部分结果。

## 测试策略

不要只 mock Mapper。应使用真实 MySQL/Testcontainers 验证插件组合后的最终 SQL、参数顺序、方言、事务回滚和执行计划；日志中记录 statement id 与规范化 SQL，但对密码、令牌和个人信息脱敏。

在大型电商系统中，插件只承载横切约束，库存扣减等核心不变量仍写成清晰的条件 SQL，并以影响行数和数据库约束作为最终裁决。

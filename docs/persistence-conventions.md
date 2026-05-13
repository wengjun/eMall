# 持久层规范

[文档索引](README.md)

本文记录 eMall 当前持久层的工程约定，目标是把 MyBatis-Plus 用在适合它的地方，同时保留关键 SQL 的可控性。
规则的核心不是“所有 SQL 都自动生成”，而是“简单 CRUD 交给框架，核心链路 SQL 必须强类型、显式、可审计、可测试”。

## 技术基线

- Java 17、Spring Boot、MyBatis-Plus、Flyway、MySQL 是核心交易链路默认持久化基线。
- `emall.storage=memory` 只用于本地实验或极轻量测试，不作为生产路径。
- 每个服务拥有自己的 schema 和 Flyway 迁移目录，避免多个服务共享表所有权。
- Outbox、支付流水、订单状态、库存预占等核心数据必须落库，不能只依赖缓存。

## MyBatis-Plus 自动配置

公共模块统一注册 `MybatisPlusInterceptor`，业务模块不需要重复配置：

```java
@Bean
@ConditionalOnMissingBean
public MybatisPlusInterceptor mybatisPlusInterceptor(
        @Value("${emall.mybatis-plus.illegal-sql-check:false}") boolean illegalSqlCheckEnabled) {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
    if (illegalSqlCheckEnabled) {
        interceptor.addInnerInterceptor(new IllegalSQLInnerInterceptor());
    }
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
}
```

拦截器职责：

- `OptimisticLockerInnerInterceptor`：支持 `@Version` 乐观锁，适合价格版本、配置版本、库存版本等并发更新场景。
- `BlockAttackInnerInterceptor`：阻止没有条件的全表 `UPDATE` 或 `DELETE`，降低误操作风险。
- `IllegalSQLInnerInterceptor`：可选开启，用于更严格的 SQL 风险检查，默认关闭，避免误拦截合法的运维或分析查询。
- `PaginationInnerInterceptor`：统一分页方言为 MySQL。

## 审计字段自动填充

实体中如果存在 `createdAt` 和 `updatedAt`，必须使用 MyBatis-Plus 的填充策略：

```java
@TableField(value = "created_at", fill = FieldFill.INSERT)
private LocalDateTime createdAt;

@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updatedAt;
```

公共模块提供 `MetaObjectHandler`：

- 插入时自动填充 `createdAt` 和 `updatedAt`。
- 更新时自动填充 `updatedAt`。
- 时间使用 UTC，避免多时区部署时出现本地时间不一致。
- 如果业务代码显式设置字段值，严格填充不会覆盖已有值。

## Mapper 规则

业务 mapper 必须遵守以下规则：

- 不使用 `Map<String, Object>` 承接数据库行。
- 不使用公共 `RowMaps` 做弱类型字段转换。
- 不写 `SELECT *`，所有查询必须显式列出字段。
- 查询结果优先返回实体、领域 record 或专用投影类型。
- 自定义 SQL 允许存在，但必须保持字段顺序、字段名和返回类型一致。

示例：

```java
@Select("""
        SELECT rule_id, scenario, discount_type, discount_value, starts_at, ends_at
        FROM promotion_rules
        WHERE scenario = #{scenario}
        ORDER BY starts_at DESC
        """)
List<PromotionRule> findRules(String scenario);
```

不推荐：

```java
@Select("SELECT * FROM promotion_rules WHERE scenario = #{scenario}")
List<Map<String, Object>> findRules(String scenario);
```

原因：

- `SELECT *` 会让字段顺序和字段变化变得不可控。
- `Map<String, Object>` 会把类型错误推迟到运行时。
- 弱类型映射容易隐藏字段缺失、字段重命名和时间类型转换问题。
- 面向生产排障时，显式 SQL 更容易结合执行计划、索引和慢查询定位问题。

## BaseMapper 和手写 SQL 的边界

优先使用 `BaseMapper` 的场景：

- 单表主键查询。
- 单表插入、更新、删除。
- 简单条件查询。
- 后台配置、字典、运营数据维护。

继续保留手写 SQL 的场景：

- 需要明确锁范围和索引命中的核心交易链路。
- 需要聚合、排序、窗口限制或复杂过滤的查询。
- 需要保证幂等、状态机流转或版本条件更新的 SQL。
- 需要和历史表结构、投影模型、Outbox 模型精确对齐的 SQL。

工程取舍：

- MyBatis-Plus 用来减少样板代码。
- 手写 SQL 用来保证高并发关键路径的可控性。
- 两者不是替代关系，而是分层使用。

## 乐观锁约定

需要并发保护的实体字段使用 `@Version`：

```java
@Version
@TableField("version")
private long version;
```

适用场景：

- 价格版本发布。
- 配置版本变更。
- 秒杀活动状态流转。
- 需要避免覆盖更新的运营后台数据。

注意：

- 乐观锁失败后不能静默忽略，业务层必须返回冲突、重试或重新读取最新数据。
- 高冲突热点写入不能只依赖乐观锁，必要时要结合分桶、队列化、限流或库存中心。

## 仍允许的非 MyBatis-Plus 场景

当前工程不追求消灭所有非 MyBatis-Plus 代码，以下场景可以保留：

- ClickHouse 分析型写入和查询。
- Kafka、Outbox、JSON payload 等事件数据结构中的 `Map<String, Object>`。
- smoke、loadtest、测试夹具中的请求响应 payload。
- Testcontainers 和基础设施测试配置。

这些场景不是业务关系型数据库行映射，不属于本次迁移范围。

## 自动化校验

公共模块增加了持久层约定测试：

```powershell
mvn -pl common test
```

测试覆盖：

- mapper 和 MyBatis-Plus repository 不能使用 `Map<String, Object>` 或 `RowMaps`。
- mapper 不能出现 `SELECT *`。
- 实体中的 `createdAt` 和 `updatedAt` 必须带自动填充注解。
- MyBatis-Plus 自动配置必须包含分页、乐观锁、防全表更新和审计字段处理器。

全量验证建议：

```powershell
mvn -DskipTests -DskipITs test-compile
mvn -DskipITs test
mvn -DskipITs=false verify
git diff --check
```

Docker 型集成测试会先做 Testcontainers Docker API 预检。默认超时时间是 10 秒，可以通过
`-Demall.testcontainers.docker-check-timeout-seconds=30` 调整。这样 Docker Desktop 或 named pipe 卡住时，测试会跳过，
不会让 Maven 构建无限挂起。

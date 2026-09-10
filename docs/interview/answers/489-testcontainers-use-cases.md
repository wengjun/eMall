# 489 Java 集成测试如何接入 Testcontainers？

[返回按分类学习面试题](../README.md)

## 学装配代码，不重复测试策略

以下示例使用 JUnit Jupiter、Spring Boot Test、Testcontainers 1.x 的 junit-jupiter/mysql 模块和 MySQL JDBC 驱动。
测试项目需已有 @SpringBootApplication 及数据源自动配置，Docker 也必须可用。

```java
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class DatabaseConnectionIT {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    DataSource dataSource;

    @Test
    void connectsToContainer() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(2));
        }
    }
}
```

镜像标签只是固定的示例依赖，不是生产版本推荐。
动态注入连接信息，不能硬编码 localhost:3306。
static @Container 通常按测试类共享生命周期，非 static 字段通常逐测试启动，
见 [Testcontainers JUnit 5 集成](https://java.testcontainers.org/test_framework_integration/junit_5/)。

## 接下来验证 MyBatis 的实际行为

上面只证明 Spring 数据源接到了容器，**不等于已测试 Mapper、事务或索引**。
真正的持久化测试应调用 Mapper，断言映射、条件更新影响行数和事务后的实际数据。
验证提交后的可见性时，要注意测试方法本身的 @Transactional 自动回滚可能掩盖问题。

`*IT` 需要 Maven Failsafe 绑定 integration-test/verify 才会按该约定运行；
单元测试命名与执行通常交给 Surefire。没有 Docker 时跳过不等于测试通过。

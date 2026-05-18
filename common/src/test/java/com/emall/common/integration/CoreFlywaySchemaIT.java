package com.emall.common.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;

@EnabledIf("dockerIsAvailable")
class CoreFlywaySchemaIT {
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4").withDatabaseName("emall_schema_it")
            .withUsername("root").withPassword("emall").withStartupTimeout(Duration.ofMinutes(2));

    private static final List<SchemaTarget> SCHEMAS = List.of(new SchemaTarget("emall_user_it", "user", 2),
            new SchemaTarget("emall_product_it", "product", 3), new SchemaTarget("emall_inventory_it", "inventory", 2),
            new SchemaTarget("emall_order_it", "order", 4), new SchemaTarget("emall_payment_it", "payment", 3),
            new SchemaTarget("emall_search_it", "search", 2),
            new SchemaTarget("emall_fulfillment_it", "fulfillment", 2));

    @BeforeAll
    static void startMysql() {
        mysql.start();
    }

    @AfterAll
    static void stopMysql() {
        mysql.stop();
    }

    static boolean dockerIsAvailable() {
        return DockerIntegrationSupport.isDockerAvailable();
    }

    @Test
    void shouldApplyCoreServiceFlywayMigrationsOnMysql() {
        JdbcTemplate admin = new JdbcTemplate(dataSource("emall_schema_it"));

        for (SchemaTarget schema : SCHEMAS) {
            admin.execute("create database if not exists " + schema.databaseName());
            MigrateResult result = Flyway.configure()
                    .dataSource(jdbcUrl(schema.databaseName()), mysql.getUsername(), mysql.getPassword())
                    .locations("filesystem:" + migrationPath(schema.moduleName())).load().migrate();

            assertThat(result.migrationsExecuted).isEqualTo(schema.expectedMigrations());
            assertThat(appliedMigrationCount(schema.databaseName())).isEqualTo(schema.expectedMigrations());
        }
    }

    private int appliedMigrationCount(String databaseName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource(databaseName));
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from flyway_schema_history
                where success = true and type = 'SQL'
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private String migrationPath(String moduleName) {
        return Path.of("..", moduleName, "src/main/resources/db/migration").toAbsolutePath().normalize().toString();
    }

    private DataSource dataSource(String databaseName) {
        return DataSourceBuilder.create().type(DriverManagerDataSource.class).url(jdbcUrl(databaseName))
                .username(mysql.getUsername()).password(mysql.getPassword()).build();
    }

    private String jdbcUrl(String databaseName) {
        return "jdbc:mysql://" + mysql.getHost() + ":" + mysql.getMappedPort(3306) + "/" + databaseName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private record SchemaTarget(String databaseName, String moduleName, int expectedMigrations) {
    }
}

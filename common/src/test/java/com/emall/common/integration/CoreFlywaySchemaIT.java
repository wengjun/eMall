package com.emall.common.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
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

    private static final Path REPOSITORY_ROOT = Path.of("..").toAbsolutePath().normalize();

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
    void shouldApplyAllServiceFlywayMigrationsOnMysql() {
        JdbcTemplate admin = new JdbcTemplate(dataSource("emall_schema_it"));

        for (SchemaTarget schema : schemaTargets()) {
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
        return REPOSITORY_ROOT.resolve(moduleName).resolve("src/main/resources/db/migration").toString();
    }

    private DataSource dataSource(String databaseName) {
        return DataSourceBuilder.create().type(DriverManagerDataSource.class).url(jdbcUrl(databaseName))
                .username(mysql.getUsername()).password(mysql.getPassword()).build();
    }

    private String jdbcUrl(String databaseName) {
        return "jdbc:mysql://" + mysql.getHost() + ":" + mysql.getMappedPort(3306) + "/" + databaseName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private List<SchemaTarget> schemaTargets() {
        try (Stream<Path> modules = Files.list(REPOSITORY_ROOT)) {
            return modules.filter(path -> Files.isDirectory(path.resolve("src/main/resources/db/migration")))
                    .map(this::schemaTarget).sorted(Comparator.comparing(SchemaTarget::moduleName)).toList();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to discover Flyway migration targets", ex);
        }
    }

    private SchemaTarget schemaTarget(Path modulePath) {
        String moduleName = modulePath.getFileName().toString();
        Path migrationDirectory = modulePath.resolve("src/main/resources/db/migration");
        try (Stream<Path> migrations = Files.list(migrationDirectory)) {
            int migrationCount = Math.toIntExact(
                    migrations.filter(path -> path.getFileName().toString().matches("V\\d+__.*\\.sql")).count());
            return new SchemaTarget("emall_" + moduleName.replace('-', '_') + "_it", moduleName, migrationCount);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to count Flyway migrations for " + moduleName, ex);
        }
    }

    private record SchemaTarget(String databaseName, String moduleName, int expectedMigrations) {
    }
}

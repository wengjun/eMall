package com.emall.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

class MigrationTargetExecutorIT {
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4").withDatabaseName("emall_user")
            .withUsername("migration_user").withPassword("migration_password");

    private final MigrationTargetExecutor executor = new MigrationTargetExecutor();

    @BeforeAll
    static void startMySql() {
        requireDocker();
        MYSQL.start();
    }

    @AfterAll
    static void stopMySql() {
        if (MYSQL.isRunning()) {
            MYSQL.stop();
        }
    }

    @Test
    void migratesPackagedAssetsAndCreatesPhysicalTablesIdempotently() throws SQLException {
        MigrationTarget target = new MigrationTarget("user", "default", 0, MYSQL.getJdbcUrl(), MYSQL.getUsername(),
                MYSQL.getPassword(), List.of("classpath:migrations/user/src/main/resources/db/migration"),
                "flyway_schema_history", "integration-test", false, false, true,
                List.of(new PhysicalTableRule("user_account", "user_account", 2, "cell-a")));

        executor.execute(target);
        executor.execute(target);

        assertThat(tableExists("flyway_schema_history")).isTrue();
        assertThat(tableExists("user_account")).isTrue();
        assertThat(tableExists("user_account_00")).isTrue();
        assertThat(tableExists("user_account_01")).isTrue();
        assertThat(appliedMigrationCount()).isEqualTo(4);
    }

    @Test
    void failsWhenMigrationAssetsAreMissing() {
        MigrationTarget target = new MigrationTarget("user", "default", 0, MYSQL.getJdbcUrl(), MYSQL.getUsername(),
                MYSQL.getPassword(), List.of("classpath:migrations/missing"), "missing_history", "integration-test",
                false, false, false, List.of());

        assertThatThrownBy(() -> executor.execute(target)).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("locations");
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection connection = connection();
                ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, tableName,
                        new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private int appliedMigrationCount() throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND type = 'SQL'")) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private static void requireDocker() {
        boolean available = DockerClientFactory.instance().isDockerAvailable();
        if (!available && Boolean.getBoolean("emall.integration.require-docker")) {
            throw new IllegalStateException("Docker is required for production integration tests");
        }
        Assumptions.assumeTrue(available, "Docker is unavailable");
    }
}

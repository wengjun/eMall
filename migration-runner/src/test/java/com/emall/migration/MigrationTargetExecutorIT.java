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
    void migratesServiceScopedAssetAndCreatesPhysicalTablesIdempotently() throws SQLException {
        MigrationTarget target = target(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword(),
                List.of("classpath:db/migration"), "flyway_schema_history", true);

        executor.execute(target);
        executor.execute(target);

        assertThat(tableExists("flyway_schema_history")).isTrue();
        assertThat(tableExists("user_account")).isTrue();
        assertThat(tableExists("user_account_00")).isTrue();
        assertThat(tableExists("user_account_01")).isTrue();
        assertThat(appliedMigrationCount()).isEqualTo(1);
    }

    @Test
    void failsWhenMigrationAssetsAreMissing() {
        MigrationTarget target = target(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword(),
                List.of("classpath:migrations/missing"), "missing_history", false);

        assertThatThrownBy(() -> executor.execute(target)).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("locations");
    }

    @Test
    void serviceMigrationCredentialCannotAccessAnotherServiceDatabase() throws SQLException {
        try (Connection root = DriverManager.getConnection(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword());
                Statement statement = root.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS emall_payment");
            statement.execute("CREATE USER IF NOT EXISTS 'user_migration_scoped'@'%' IDENTIFIED BY 'scoped_secret'");
            statement.execute("GRANT ALL PRIVILEGES ON emall_user.* TO 'user_migration_scoped'@'%'");
        }

        String scopedUserUrl = MYSQL.getJdbcUrl();
        try (Connection ignored =
                DriverManager.getConnection(scopedUserUrl, "user_migration_scoped", "scoped_secret")) {
            assertThat(ignored.isValid(2)).isTrue();
        }
        String paymentUrl = scopedUserUrl.replace("/emall_user", "/emall_payment");
        assertThatThrownBy(() -> DriverManager.getConnection(paymentUrl, "user_migration_scoped", "scoped_secret"))
                .isInstanceOf(SQLException.class);
    }

    private MigrationTarget target(String jdbcUrl, String username, String password, List<String> locations,
            String historyTable, boolean createPhysicalTables) {
        return new MigrationTarget("user", "default", 0, "emall_user", jdbcUrl, username, password, locations,
                historyTable, "integration-test", "it-batch", false, false, createPhysicalTables,
                createPhysicalTables
                        ? List.of(new PhysicalTableRule("user_account", "user_account", 2, "cell-a"))
                        : List.of(),
                MigrationPhase.EXPAND, false, "", "");
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

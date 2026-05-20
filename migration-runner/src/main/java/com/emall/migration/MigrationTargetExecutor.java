package com.emall.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MigrationTargetExecutor {
    private static final Logger log = LoggerFactory.getLogger(MigrationTargetExecutor.class);

    public void execute(MigrationTarget target) {
        if (target.dryRun()) {
            log.info("Dry-run schema migration service={} region={} shard={} locations={} jdbcUrl={}", target.service(),
                    target.region(), target.shard(), target.locations(), maskJdbcUrl(target.jdbcUrl()));
            return;
        }
        FluentConfiguration configuration =
                Flyway.configure().dataSource(target.jdbcUrl(), target.username(), target.password())
                        .locations(target.locations().toArray(String[]::new)).table(target.historyTable())
                        .baselineOnMigrate(target.baselineOnMigrate())
                        .placeholders(Map.of("service", target.service(), "region", target.region(), "shard",
                                Integer.toString(target.shard()), "operator", target.operator()));
        var result = configuration.load().migrate();
        log.info("Schema migration completed service={} region={} shard={} migrationsExecuted={} targetSchema={}",
                target.service(), target.region(), target.shard(), result.migrationsExecuted, result.schemaName);
        createPhysicalTables(target);
    }

    private void createPhysicalTables(MigrationTarget target) {
        if (!target.createPhysicalTables() || target.physicalTables().isEmpty()) {
            return;
        }
        try (Connection connection =
                DriverManager.getConnection(target.jdbcUrl(), target.username(), target.password());
                Statement statement = connection.createStatement()) {
            for (PhysicalTableRule rule : target.physicalTables()) {
                for (int index = 0; index < rule.tableShardCount(); index++) {
                    String tableName = "%s_%02d".formatted(rule.tablePrefix(), index);
                    String createSql = "CREATE TABLE IF NOT EXISTS " + tableName + " LIKE " + rule.sourceTable();
                    statement.execute(createSql);
                    log.info("Ensured physical table service={} region={} shard={} table={} source={}",
                            target.service(), target.region(), target.shard(), tableName, rule.sourceTable());
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to create physical sharding tables for " + target.service(), ex);
        }
    }

    private String maskJdbcUrl(String jdbcUrl) {
        int queryStart = jdbcUrl.indexOf('?');
        return queryStart < 0 ? jdbcUrl : jdbcUrl.substring(0, queryStart);
    }
}

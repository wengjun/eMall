package com.emall.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationRunnerPropertiesTest {
    @Test
    void expandsOnlyOneServiceAndPlansCanaryBeforeBoundedBatches() {
        MigrationRunnerProperties properties = validProperties("order");
        properties.setRegion("cn-east");
        properties.setShards(List.of(7, 0, 3, 2, 1, 6, 5, 4));
        properties.setCanaryShardCount(1);
        properties.setBatchSize(3);

        List<List<MigrationTarget>> batches = properties.planBatches();

        assertThat(batches).hasSize(4);
        assertThat(batches.get(0)).extracting(MigrationTarget::shard).containsExactly(0);
        assertThat(batches.get(1)).extracting(MigrationTarget::shard).containsExactly(1, 2, 3);
        assertThat(batches.get(2)).extracting(MigrationTarget::shard).containsExactly(4, 5, 6);
        assertThat(batches.get(3)).extracting(MigrationTarget::shard).containsExactly(7);
        assertThat(batches.stream().flatMap(List::stream)).allSatisfy(target -> {
            assertThat(target.service()).isEqualTo("order");
            assertThat(target.region()).isEqualTo("cn-east");
            assertThat(target.jdbcUrl()).contains("/emall_order_%02d".formatted(target.shard()));
        });
    }

    @Test
    void usesUnsuffixedDatabaseForAnUnshardedService() {
        MigrationRunnerProperties properties = validProperties("analytics");

        assertThat(properties.expandTargets()).singleElement().satisfies(target -> {
            assertThat(target.database()).isEqualTo("emall_analytics");
            assertThat(target.jdbcUrl()).contains("/emall_analytics?");
        });
    }

    @Test
    void rejectsJdbcUrlThatEscapesTheServiceDatabaseBoundary() {
        MigrationRunnerProperties properties = validProperties("order");
        properties.setJdbcUrlTemplate("jdbc:mysql://mysql:3306/emall_payment?serverTimezone=UTC");

        assertThatThrownBy(properties::expandTargets).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("escapes service database boundary");
    }

    @Test
    void rejectsDatabaseCreationAndMissingServiceCredentials() {
        MigrationRunnerProperties properties = validProperties("order");
        properties.setJdbcUrlTemplate(
                "jdbc:mysql://mysql:3306/{database}?serverTimezone=UTC&createDatabaseIfNotExist=true");
        assertThatThrownBy(properties::expandTargets).hasMessageContaining("cannot create databases");

        properties.setJdbcUrlTemplate("jdbc:mysql://mysql:3306/{database}?serverTimezone=UTC");
        properties.setPassword("");
        assertThatThrownBy(properties::expandTargets).hasMessageContaining("service-scoped migration username");
    }

    @Test
    void configuresPhysicalTablesOnlyForTheSelectedService() {
        MigrationRunnerProperties properties = validProperties("payment");
        properties.setCreatePhysicalTables(true);
        properties.setDefaultTableShardCount(16);
        properties.setCellId("cell-b");

        MigrationTarget target = properties.expandTargets().get(0);

        assertThat(target.physicalTables()).extracting(PhysicalTableRule::tablePrefix).containsExactly("payment_order",
                "payment_ledger_entry", "payment_channel_statement", "payment_reconciliation_record");
        assertThat(target.physicalTables()).allSatisfy(rule -> {
            assertThat(rule.tableShardCount()).isEqualTo(16);
            assertThat(rule.cellId()).isEqualTo("cell-b");
        });
    }

    @Test
    void validatesBatchSafetyLimits() {
        MigrationRunnerProperties properties = validProperties("order");
        properties.setBatchTimeout(Duration.ZERO);

        assertThatThrownBy(properties::planBatches).hasMessageContaining("batch-timeout");
    }

    private MigrationRunnerProperties validProperties(String service) {
        MigrationRunnerProperties properties = new MigrationRunnerProperties();
        properties.setService(service);
        properties.setJdbcUrlTemplate("jdbc:mysql://mysql:3306/{database}?serverTimezone=UTC");
        properties.setUsername(service.replace('-', '_') + "_migration");
        properties.setPassword("secret");
        properties.setLocations(List.of("filesystem:/app/migrations"));
        return properties;
    }
}

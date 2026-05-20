package com.emall.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationRunnerPropertiesTest {
    @Test
    void shouldExpandServiceRegionShardTargets() {
        MigrationRunnerProperties properties = new MigrationRunnerProperties();
        properties.setServices(List.of("order", "payment"));
        properties.setRegions(List.of("cn-east", "cn-south"));
        properties.setShards(List.of(0, 1));
        properties.setJdbcUrlTemplate(
                "jdbc:mysql://mysql-{region}:3306/emall_{service}_{shard}?useUnicode=true&serverTimezone=UTC");
        properties.setLocations(List.of("filesystem:/migrations/{service}"));
        properties.setOperator("release-bot");

        List<MigrationTarget> targets = properties.expandTargets();

        assertThat(targets).hasSize(8);
        assertThat(targets).anySatisfy(target -> {
            assertThat(target.jdbcUrl())
                    .isEqualTo("jdbc:mysql://mysql-cn-east:3306/emall_payment_1?useUnicode=true&serverTimezone=UTC");
            assertThat(target.locations()).containsExactly("filesystem:/migrations/payment");
            assertThat(target.operator()).isEqualTo("release-bot");
        });
    }

    @Test
    void shouldRequireExplicitServices() {
        MigrationRunnerProperties properties = new MigrationRunnerProperties();
        properties.setJdbcUrlTemplate("jdbc:mysql://localhost:3306/emall_{service}");

        assertThatThrownBy(properties::expandTargets).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("services");
    }

    @Test
    void shouldExpandDefaultPhysicalTableRulesForCoreServices() {
        MigrationRunnerProperties properties = new MigrationRunnerProperties();
        properties.setServices(List.of("order", "payment", "search"));
        properties.setJdbcUrlTemplate("jdbc:mysql://localhost:3306/emall_{service}_{shard}");
        properties.setCreatePhysicalTables(true);
        properties.setDefaultTableShardCount(16);
        properties.setCellId("cell-b");

        List<MigrationTarget> targets = properties.expandTargets();

        assertThat(targets).hasSize(3);
        assertThat(targets).anySatisfy(target -> {
            assertThat(target.service()).isEqualTo("order");
            assertThat(target.createPhysicalTables()).isTrue();
            assertThat(target.physicalTables()).extracting(PhysicalTableRule::tablePrefix)
                    .containsExactly("order_record", "outbox_event");
            assertThat(target.physicalTables()).allSatisfy(rule -> {
                assertThat(rule.tableShardCount()).isEqualTo(16);
                assertThat(rule.cellId()).isEqualTo("cell-b");
            });
        });
        assertThat(targets).anySatisfy(target -> {
            assertThat(target.service()).isEqualTo("payment");
            assertThat(target.physicalTables()).extracting(PhysicalTableRule::tablePrefix).contains("payment_order",
                    "payment_refund_order");
        });
        assertThat(targets).anySatisfy(target -> {
            assertThat(target.service()).isEqualTo("search");
            assertThat(target.physicalTables()).extracting(PhysicalTableRule::tablePrefix)
                    .containsExactly("search_document", "processed_message");
        });
    }
}

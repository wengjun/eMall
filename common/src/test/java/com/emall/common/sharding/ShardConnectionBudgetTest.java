package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShardConnectionBudgetTest {
    @Test
    void shouldAllocateAtMostSixtyFourConnectionsForSixteenShardPools() {
        ShardDataSourceProperties properties = new ShardDataSourceProperties();

        Map<String, ShardConnectionBudget.PoolAllocation> plan = ShardConnectionBudget.plan(properties, shardSpecs(16));

        assertThat(plan).hasSize(16);
        assertThat(plan.values()).allSatisfy(allocation -> {
            assertThat(allocation.maximumPoolSize()).isEqualTo(4);
            assertThat(allocation.minimumIdle()).isZero();
        });
        assertThat(plan.values().stream().mapToInt(ShardConnectionBudget.PoolAllocation::maximumPoolSize).sum())
                .isEqualTo(64);
    }

    @Test
    void shouldRejectPodDatabaseAndGlobalBudgetViolationsAtStartup() {
        ShardDataSourceProperties podBudget = new ShardDataSourceProperties();
        podBudget.setPodConnectionBudget(63);
        assertThatThrownBy(() -> ShardConnectionBudget.plan(podBudget, shardSpecs(16)))
                .hasMessageContaining("pod connection budget");

        ShardDataSourceProperties databaseBudget = new ShardDataSourceProperties();
        databaseBudget.setDatabaseInstanceConnectionBudget(49);
        assertThatThrownBy(() -> ShardConnectionBudget.plan(databaseBudget, shardSpecs(16)))
                .hasMessageContaining("database instance connection budget");

        ShardDataSourceProperties globalBudget = new ShardDataSourceProperties();
        globalBudget.setGlobalConnectionBudget(799);
        assertThatThrownBy(() -> ShardConnectionBudget.plan(globalBudget, shardSpecs(16)))
                .hasMessageContaining("global connection budget");
    }

    @Test
    void shouldHonorSmallerExplicitPoolOverrides() {
        ShardDataSourceProperties properties = new ShardDataSourceProperties();
        Map<String, ShardDataSourceProperties.DataSourceSpec> specs = shardSpecs(2);
        specs.get("db-0").setMaximumPoolSize(2);
        specs.get("db-0").setMinimumIdle(1);

        Map<String, ShardConnectionBudget.PoolAllocation> plan = ShardConnectionBudget.plan(properties, specs);

        assertThat(plan.get("db-0")).isEqualTo(new ShardConnectionBudget.PoolAllocation(2, 1));
        assertThat(plan.get("db-1")).isEqualTo(new ShardConnectionBudget.PoolAllocation(4, 0));
    }

    private Map<String, ShardDataSourceProperties.DataSourceSpec> shardSpecs(int count) {
        Map<String, ShardDataSourceProperties.DataSourceSpec> specs = new LinkedHashMap<>();
        for (int index = 0; index < count; index++) {
            specs.put("db-" + index, new ShardDataSourceProperties.DataSourceSpec());
        }
        return specs;
    }
}

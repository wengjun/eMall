package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StaticVirtualShardPlacementProviderTest {
    @Test
    void preservesExistingModuloLocationsBehindAStableVirtualShard() {
        ShardRoutingProperties properties = properties();
        StaticVirtualShardPlacementProvider provider = new StaticVirtualShardPlacementProvider(properties);
        long shardKey = 123_456_789L;
        int virtualShard = Math.floorMod(shardKey, properties.getVirtualShardCount());

        PhysicalShardPlacement placement = provider.resolve("order", virtualShard, ShardAccessMode.READ).primary();

        int expectedDatabase = Math.floorMod(shardKey, properties.getDatabaseShardCount());
        int expectedTable = Math.floorMod(Math.floorDiv(shardKey, properties.getDatabaseShardCount()), 64);
        assertThat(placement.databaseName()).isEqualTo("emall_order_%02d".formatted(expectedDatabase));
        assertThat(placement.resolveTableName("order_record")).isEqualTo("order_record_%02d".formatted(expectedTable));
    }

    @Test
    void enumeratesEachPhysicalDestinationOnceForBackgroundWork() {
        StaticVirtualShardPlacementProvider provider = new StaticVirtualShardPlacementProvider(properties());

        assertThat(provider.activePhysicalPlacements("order", "order_record", ShardAccessMode.WRITE)).hasSize(512);
    }

    @Test
    void rejectsTopologyThatWouldRemapVirtualShardsImplicitly() {
        ShardRoutingProperties properties = properties();
        properties.setVirtualShardCount(1024);
        properties.setDatabaseShardCount(10);

        assertThatThrownBy(() -> new StaticVirtualShardPlacementProvider(properties))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("divisible by physical slots");
    }

    private ShardRoutingProperties properties() {
        ShardRoutingProperties properties = new ShardRoutingProperties();
        properties.setEnabled(true);
        properties.setDatabasePrefix("emall_order");
        properties.setDatabaseShardCount(8);
        properties.setVirtualShardCount(4096);
        properties.setDefaultRegionId("cn-east-1");
        properties.setDefaultCellId("cell-a");
        properties.getTables().put("order_record", new ShardRoutingProperties.TableRule("order_record", 64));
        return properties;
    }
}

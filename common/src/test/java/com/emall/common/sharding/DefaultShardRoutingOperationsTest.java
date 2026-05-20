package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefaultShardRoutingOperationsTest {
    @Test
    void shouldRouteStableShardAndPhysicalTable() {
        ShardRoutingProperties properties = properties();
        DefaultShardRoutingOperations operations = new DefaultShardRoutingOperations(properties);

        ShardRoutingDecision first = operations.decide("order_record", 10001L);
        ShardRoutingDecision second = operations.decide("order_record", 10001L);

        assertThat(second).isEqualTo(first);
        assertThat(first.logicalShard()).isBetween(0, 63);
        assertThat(first.databaseName()).startsWith("emall_order_");
        assertThat(first.resolveTableName("order_record")).startsWith("order_record_");
        assertThat(first.cellId()).isEqualTo("cell-a");
    }

    @Test
    void shouldCoverConfiguredLogicalShards() {
        ShardRoutingProperties properties = properties();
        DefaultShardRoutingOperations operations = new DefaultShardRoutingOperations(properties);
        Set<Integer> shards = new HashSet<>();

        for (long key = 0; key < 4096; key++) {
            shards.add(operations.decide("order_record", key).logicalShard());
        }

        assertThat(shards).hasSize(64);
    }

    @Test
    void shouldBindAndRestoreShardContext() {
        ShardRoutingProperties properties = properties();
        DefaultShardRoutingOperations operations = new DefaultShardRoutingOperations(properties);

        String physicalTable =
                operations.execute("order_record", 42L, () -> ShardContext.resolveTableName("order_record"));

        assertThat(physicalTable).startsWith("order_record_");
        assertThat(ShardContext.current()).isEmpty();
    }

    private ShardRoutingProperties properties() {
        ShardRoutingProperties properties = new ShardRoutingProperties();
        properties.setEnabled(true);
        properties.setDatabasePrefix("emall_order");
        properties.setDatabaseShardCount(8);
        properties.setLogicalShardCount(64);
        properties.getTables().put("order_record", new ShardRoutingProperties.TableRule("order_record", 64));
        return properties;
    }
}

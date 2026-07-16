package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        assertThat(first.logicalShard()).isBetween(0, 4095);
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

        assertThat(shards).hasSize(4096);
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

    @Test
    void fencesWritesDuringCutoverButKeepsReadsOnTheCurrentPrimary() {
        ShardRoutingProperties properties = properties();
        PhysicalShardPlacement primary = new PhysicalShardPlacement("emall_order_01", 1, "cn-east-1", "cell-a",
                Map.of("order_record", "order_record_01"));
        PhysicalShardPlacement target = new PhysicalShardPlacement("emall_order_08", 8, "cn-east-1", "cell-b",
                Map.of("order_record", "order_record_01"));
        VirtualShardPlacement placement = new VirtualShardPlacement(properties.mappingNamespace(), 1, 2L, 7L,
                ShardMigrationState.CUTOVER_PENDING, primary, target, Instant.now().plusSeconds(60), Instant.now());
        VirtualShardPlacementProvider provider = new VirtualShardPlacementProvider() {
            @Override
            public VirtualShardPlacement resolve(String namespace, int virtualShard, ShardAccessMode accessMode) {
                return placement;
            }

            @Override
            public List<PhysicalShardPlacement> activePhysicalPlacements(String namespace, String logicalTable,
                    ShardAccessMode accessMode) {
                return List.of(primary);
            }
        };
        DefaultShardRoutingOperations operations = new DefaultShardRoutingOperations(properties, provider);

        assertThatThrownBy(() -> operations.execute("order_record", 1L, () -> "write"))
                .isInstanceOf(ShardWriteFencedException.class);
        assertThat(
                operations.executeRead("order_record", 1L, () -> ShardContext.current().orElseThrow().databaseName()))
                .isEqualTo("emall_order_01");
    }

    private ShardRoutingProperties properties() {
        ShardRoutingProperties properties = new ShardRoutingProperties();
        properties.setEnabled(true);
        properties.setDatabasePrefix("emall_order");
        properties.setDatabaseShardCount(8);
        properties.setVirtualShardCount(4096);
        properties.setDefaultRegionId("cn-east-1");
        properties.getTables().put("order_record", new ShardRoutingProperties.TableRule("order_record", 64));
        return properties;
    }
}

package com.emall.common.region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.emall.common.sharding.PhysicalShardPlacement;
import com.emall.common.sharding.ShardAccessMode;
import com.emall.common.sharding.ShardMigrationState;
import com.emall.common.sharding.ShardRoutingProperties;
import com.emall.common.sharding.ShardWriteFencedException;
import com.emall.common.sharding.VirtualShardPlacement;
import com.emall.common.sharding.VirtualShardPlacementProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OwnershipGuardTest {
    @Test
    void shouldAcceptOwnerRegionAndCell() {
        OwnershipGuard guard = new OwnershipGuard(properties("cn-east-1", "cell-a", RegionWriteStatus.ACTIVE));

        OwnershipDecision decision = guard.checkWrite("order", 2L);

        assertThat(decision.accepted()).isTrue();
        assertThat(decision.ownerRegion()).isEqualTo("cn-east-1");
        assertThat(decision.ownerCell()).isEqualTo("cell-a");
    }

    @Test
    void shouldRejectNonOwnerRegion() {
        OwnershipGuard guard = new OwnershipGuard(properties("cn-south-1", "cell-a", RegionWriteStatus.ACTIVE));

        assertThatThrownBy(() -> guard.checkWrite("order", 2L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("ownerRegion=cn-east-1");
    }

    @Test
    void shouldRejectReadOnlyRegion() {
        OwnershipGuard guard = new OwnershipGuard(properties("cn-east-1", "cell-a", RegionWriteStatus.READ_ONLY));

        assertThatThrownBy(() -> guard.checkWrite("order", 2L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("READ_ONLY");
    }

    @Test
    void usesAuthoritativeVirtualShardPlacementForRegionAndCellOwnership() {
        OwnershipProperties ownership = properties("cn-north-1", "cell-b", RegionWriteStatus.ACTIVE);
        ShardRoutingProperties sharding = shardingProperties();
        PhysicalShardPlacement physical =
                new PhysicalShardPlacement("emall_order_08", 8, "cn-north-1", "cell-b", Map.of("orders", "orders_00"));
        VirtualShardPlacement placement = new VirtualShardPlacement("order", 2, 7L, 2L, ShardMigrationState.STABLE,
                physical, null, null, Instant.parse("2026-07-15T10:00:00Z"));
        OwnershipGuard guard = new OwnershipGuard(ownership, sharding, provider(placement));

        OwnershipDecision decision = guard.checkWrite("order", 2L);

        assertThat(decision.accepted()).isTrue();
        assertThat(decision.ownerRegion()).isEqualTo("cn-north-1");
        assertThat(decision.ownerCell()).isEqualTo("cell-b");
    }

    @Test
    void propagatesVirtualShardCutoverWriteFence() {
        OwnershipProperties ownership = properties("cn-east-1", "cell-a", RegionWriteStatus.ACTIVE);
        ShardRoutingProperties sharding = shardingProperties();
        VirtualShardPlacementProvider provider = new VirtualShardPlacementProvider() {
            @Override
            public VirtualShardPlacement resolve(String namespace, int virtualShard, ShardAccessMode accessMode) {
                throw new ShardWriteFencedException(namespace, virtualShard, 7L, 2L,
                        ShardMigrationState.CUTOVER_PENDING);
            }

            @Override
            public List<PhysicalShardPlacement> activePhysicalPlacements(String namespace, String logicalTable,
                    ShardAccessMode accessMode) {
                return List.of();
            }
        };

        assertThatThrownBy(() -> new OwnershipGuard(ownership, sharding, provider).checkWrite("order", 2L))
                .isInstanceOf(ShardWriteFencedException.class);
    }

    private ShardRoutingProperties shardingProperties() {
        ShardRoutingProperties properties = new ShardRoutingProperties();
        properties.setEnabled(true);
        properties.setMappingNamespace("order");
        properties.setVirtualShardCount(4096);
        return properties;
    }

    private VirtualShardPlacementProvider provider(VirtualShardPlacement placement) {
        return new VirtualShardPlacementProvider() {
            @Override
            public VirtualShardPlacement resolve(String namespace, int virtualShard, ShardAccessMode accessMode) {
                return placement;
            }

            @Override
            public List<PhysicalShardPlacement> activePhysicalPlacements(String namespace, String logicalTable,
                    ShardAccessMode accessMode) {
                return List.of(placement.primary());
            }
        };
    }

    private OwnershipProperties properties(String currentRegion, String currentCell, RegionWriteStatus status) {
        OwnershipProperties properties = new OwnershipProperties();
        properties.setEnabled(true);
        properties.setCurrentRegion(currentRegion);
        properties.setCurrentCell(currentCell);
        properties.setRegionStatuses(Map.of(currentRegion, status));
        OwnershipProperties.DomainOwnership order = new OwnershipProperties.DomainOwnership();
        order.setStrategy(WriteOwnershipStrategy.GLOBAL_SINGLE_WRITER);
        order.setPrimaryRegion("cn-east-1");
        order.setOwnerRegions(List.of("cn-east-1"));
        order.setOwnerCells(List.of("cell-a"));
        properties.setDomains(Map.of("order", order));
        return properties;
    }
}

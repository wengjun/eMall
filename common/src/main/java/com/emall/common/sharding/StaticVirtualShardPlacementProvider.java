package com.emall.common.sharding;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StaticVirtualShardPlacementProvider implements VirtualShardPlacementProvider {
    private final ShardRoutingProperties properties;
    private final Clock clock;

    public StaticVirtualShardPlacementProvider(ShardRoutingProperties properties) {
        this(properties, Clock.systemUTC());
    }

    StaticVirtualShardPlacementProvider(ShardRoutingProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        validateTopology();
    }

    @Override
    public VirtualShardPlacement resolve(String namespace, int virtualShard, ShardAccessMode accessMode) {
        if (virtualShard < 0 || virtualShard >= properties.getVirtualShardCount()) {
            throw new IllegalArgumentException("virtual shard is outside the configured range");
        }
        PhysicalShardPlacement primary = physicalPlacement(virtualShard);
        return new VirtualShardPlacement(namespace, virtualShard, 1L, 1L, ShardMigrationState.STABLE, primary, null,
                null, clock.instant());
    }

    @Override
    public List<PhysicalShardPlacement> activePhysicalPlacements(String namespace, String logicalTable,
            ShardAccessMode accessMode) {
        Map<String, PhysicalShardPlacement> placements = new LinkedHashMap<>();
        for (int virtualShard = 0; virtualShard < properties.getVirtualShardCount(); virtualShard++) {
            PhysicalShardPlacement placement = physicalPlacement(virtualShard);
            placements.putIfAbsent(placement.identity(logicalTable), placement);
        }
        return List.copyOf(placements.values());
    }

    private PhysicalShardPlacement physicalPlacement(int virtualShard) {
        int databaseIndex = Math.floorMod(virtualShard, properties.getDatabaseShardCount());
        String databaseName = properties.getDatabaseShardCount() == 1
                ? properties.getDatabasePrefix()
                : format(properties.getDatabasePrefix(), databaseIndex);
        Map<String, String> tables = new LinkedHashMap<>();
        properties.getTables().forEach((logicalTable, rule) -> {
            int tableIndex = Math.floorMod(Math.floorDiv(virtualShard, properties.getDatabaseShardCount()),
                    rule.getTableShardCount());
            String tableName =
                    rule.getTableShardCount() == 1 ? rule.getTablePrefix() : format(rule.getTablePrefix(), tableIndex);
            tables.put(logicalTable, tableName);
        });
        String cellId = properties.getShardCells().getOrDefault(virtualShard, properties.getDefaultCellId());
        return new PhysicalShardPlacement(databaseName, databaseIndex, properties.getDefaultRegionId(), cellId, tables);
    }

    private void validateTopology() {
        int virtualShardCount = properties.getVirtualShardCount();
        if (virtualShardCount <= 0 || Integer.bitCount(virtualShardCount) != 1) {
            throw new IllegalStateException("virtual shard count must be a positive power of two");
        }
        if (properties.getDatabaseShardCount() <= 0) {
            throw new IllegalStateException("database shard count must be positive");
        }
        properties.getTables().forEach((logicalTable, rule) -> {
            int physicalSlots = Math.multiplyExact(properties.getDatabaseShardCount(), rule.getTableShardCount());
            if (rule.getTableShardCount() <= 0 || virtualShardCount % physicalSlots != 0) {
                throw new IllegalStateException(
                        "virtual shard count must be divisible by physical slots for " + logicalTable);
            }
        });
    }

    private String format(String prefix, int index) {
        return "%s_%02d".formatted(prefix, index);
    }
}

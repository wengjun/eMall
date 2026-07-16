package com.emall.common.sharding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class DefaultShardRoutingOperations implements ShardRoutingOperations {
    private final ShardRoutingProperties properties;
    private final VirtualShardPlacementProvider placementProvider;

    public DefaultShardRoutingOperations(ShardRoutingProperties properties) {
        this(properties, new StaticVirtualShardPlacementProvider(properties));
    }

    public DefaultShardRoutingOperations(ShardRoutingProperties properties,
            VirtualShardPlacementProvider placementProvider) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.placementProvider = Objects.requireNonNull(placementProvider, "placementProvider must not be null");
    }

    @Override
    public <T> T execute(String logicalTable, long shardKey, Supplier<T> action) {
        if (!properties.isEnabled()) {
            return action.get();
        }
        ShardRoutingDecision decision = decide(logicalTable, shardKey, ShardAccessMode.WRITE);
        try (ShardScope ignored = ShardContext.use(decision)) {
            return action.get();
        }
    }

    @Override
    public <T> T execute(String logicalTable, String shardKey, Supplier<T> action) {
        return execute(logicalTable, Objects.requireNonNull(shardKey, "shardKey must not be null").hashCode(), action);
    }

    @Override
    public <T> T executeRead(String logicalTable, long shardKey, Supplier<T> action) {
        if (!properties.isEnabled()) {
            return action.get();
        }
        ShardRoutingDecision decision = decide(logicalTable, shardKey, ShardAccessMode.READ);
        try (ShardScope ignored = ShardContext.use(decision)) {
            return action.get();
        }
    }

    @Override
    public <T> T executeRead(String logicalTable, String shardKey, Supplier<T> action) {
        long hashedKey = Objects.requireNonNull(shardKey, "shardKey must not be null").hashCode();
        return executeRead(logicalTable, hashedKey, action);
    }

    @Override
    public <T> T executePhysicalShard(String logicalTable, int shardIndex, Supplier<T> action) {
        if (!properties.isEnabled()) {
            return action.get();
        }
        List<PhysicalShardPlacement> placements = placementProvider
                .activePhysicalPlacements(properties.mappingNamespace(), logicalTable, ShardAccessMode.WRITE);
        if (shardIndex < 0 || shardIndex >= placements.size()) {
            throw new IllegalArgumentException("physical shard index is outside the active placement range");
        }
        ShardRoutingDecision decision =
                decision(logicalTable, shardIndex, -1, placements.get(shardIndex), 1L, 1L, ShardMigrationState.STABLE);
        try (ShardScope ignored = ShardContext.use(decision)) {
            return action.get();
        }
    }

    @Override
    public int physicalShardCount(String logicalTable) {
        if (!properties.isEnabled()) {
            return 1;
        }
        return placementProvider
                .activePhysicalPlacements(properties.mappingNamespace(), logicalTable, ShardAccessMode.WRITE).size();
    }

    public ShardRoutingDecision decide(String logicalTable, long shardKey) {
        return decide(logicalTable, shardKey, ShardAccessMode.WRITE);
    }

    private ShardRoutingDecision decide(String logicalTable, long shardKey, ShardAccessMode accessMode) {
        int virtualShard = Math.floorMod(shardKey, properties.getVirtualShardCount());
        VirtualShardPlacement placement =
                placementProvider.resolve(properties.mappingNamespace(), virtualShard, accessMode);
        if (accessMode == ShardAccessMode.WRITE) {
            placement.requireWriteAllowed();
        }
        return decision(logicalTable, shardKey, virtualShard, placement.primary(), placement.mappingVersion(),
                placement.epoch(), placement.state());
    }

    private ShardRoutingDecision decision(String logicalTable, long shardKey, int virtualShard,
            PhysicalShardPlacement placement, long mappingVersion, long epoch, ShardMigrationState state) {
        Map<String, String> physicalTables = new LinkedHashMap<>(placement.physicalTables());
        physicalTables.putIfAbsent(logicalTable, logicalTable);
        return new ShardRoutingDecision(logicalTable, shardKey, virtualShard, placement.cellId(),
                placement.databaseName(), placement.databaseIndex(), physicalTables, mappingVersion, epoch, state);
    }
}

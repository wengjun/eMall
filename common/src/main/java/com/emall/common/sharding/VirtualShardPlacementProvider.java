package com.emall.common.sharding;

import java.util.List;

public interface VirtualShardPlacementProvider {
    VirtualShardPlacement resolve(String namespace, int virtualShard, ShardAccessMode accessMode);

    List<PhysicalShardPlacement> activePhysicalPlacements(String namespace, String logicalTable,
            ShardAccessMode accessMode);

    default void invalidate(String namespace, int virtualShard) {
    }
}

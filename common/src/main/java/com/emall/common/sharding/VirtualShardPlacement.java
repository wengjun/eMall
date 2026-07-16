package com.emall.common.sharding;

import java.time.Instant;

public record VirtualShardPlacement(String namespace, int virtualShard, long mappingVersion, long epoch,
        ShardMigrationState state, PhysicalShardPlacement primary, PhysicalShardPlacement migrationTarget,
        Instant cutoverNotBefore, Instant updatedAt) {
    public VirtualShardPlacement {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (virtualShard < 0 || mappingVersion <= 0 || epoch <= 0) {
            throw new IllegalArgumentException("virtual shard, mapping version, and epoch must be positive");
        }
        if (state == null || primary == null || updatedAt == null) {
            throw new IllegalArgumentException("state, primary placement, and updatedAt are required");
        }
        if (state.migrationActive() && migrationTarget == null) {
            throw new IllegalArgumentException("active migration state requires a migration target");
        }
    }

    public void requireWriteAllowed() {
        if (!state.writeAllowed()) {
            throw new ShardWriteFencedException(namespace, virtualShard, mappingVersion, epoch, state);
        }
    }
}

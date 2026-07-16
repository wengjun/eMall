package com.emall.common.sharding;

import java.time.Instant;
import java.util.Optional;

public interface ShardRouteDirectory {
    Optional<ShardRouteRecord> resolve(String namespace, String lookupHash);

    ShardRouteRecord bind(String namespace, String lookupHash, long shardKey, Instant expiresAt, boolean unique);

    default boolean removeIfOwned(String namespace, String lookupHash, long shardKey) {
        return removeIfOwned(namespace, lookupHash, shardKey, null);
    }

    boolean removeIfOwned(String namespace, String lookupHash, long shardKey, Long expectedVersion);

    ShardRoutePage scan(String cursor, int limit);
}

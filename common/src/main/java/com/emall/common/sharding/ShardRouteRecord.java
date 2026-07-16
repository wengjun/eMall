package com.emall.common.sharding;

import java.time.Instant;

public record ShardRouteRecord(String namespace, String lookupHash, long shardKey, long version, Instant expiresAt,
        Instant createdAt, Instant updatedAt) {
    public boolean expired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}

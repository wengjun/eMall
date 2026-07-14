package com.emall.common.sharding;

import java.util.Objects;

public final class HashModShardRouter {
    private final String databasePrefix;
    private final int databaseShardCount;
    private final String tablePrefix;
    private final int tableShardCount;

    public HashModShardRouter(String databasePrefix, int databaseShardCount, String tablePrefix, int tableShardCount) {
        if (databasePrefix == null || databasePrefix.isBlank()) {
            throw new IllegalArgumentException("databasePrefix must not be blank");
        }
        if (tablePrefix == null || tablePrefix.isBlank()) {
            throw new IllegalArgumentException("tablePrefix must not be blank");
        }
        if (databaseShardCount <= 0) {
            throw new IllegalArgumentException("databaseShardCount must be positive");
        }
        if (tableShardCount <= 0) {
            throw new IllegalArgumentException("tableShardCount must be positive");
        }
        this.databasePrefix = databasePrefix;
        this.databaseShardCount = databaseShardCount;
        this.tablePrefix = tablePrefix;
        this.tableShardCount = tableShardCount;
    }

    public ShardRoute route(long shardKey) {
        int databaseIndex = Math.floorMod(shardKey, databaseShardCount);
        int tableIndex = Math.floorMod(Math.floorDiv(shardKey, databaseShardCount), tableShardCount);
        String databaseName = databaseShardCount == 1 ? databasePrefix : format(databasePrefix, databaseIndex);
        String tableName = tableShardCount == 1 ? tablePrefix : format(tablePrefix, tableIndex);
        return new ShardRoute(databaseName, tableName, databaseIndex, tableIndex);
    }

    public ShardRoute route(String shardKey) {
        return route(Objects.requireNonNull(shardKey, "shardKey must not be null").hashCode());
    }

    private String format(String prefix, int index) {
        return "%s_%02d".formatted(prefix, index);
    }
}

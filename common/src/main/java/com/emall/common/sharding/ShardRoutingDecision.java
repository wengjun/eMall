package com.emall.common.sharding;

import java.util.Map;

public record ShardRoutingDecision(String logicalTable, long shardKey, int logicalShard, String cellId,
        String databaseName, int databaseIndex, Map<String, String> physicalTables, long mappingVersion, long epoch,
        ShardMigrationState migrationState) {
    public ShardRoutingDecision {
        physicalTables = Map.copyOf(physicalTables);
    }

    public ShardRoutingDecision(String logicalTable, long shardKey, int logicalShard, String cellId,
            String databaseName, int databaseIndex, Map<String, String> physicalTables) {
        this(logicalTable, shardKey, logicalShard, cellId, databaseName, databaseIndex, physicalTables, 1L, 1L,
                ShardMigrationState.STABLE);
    }

    public String resolveTableName(String tableName) {
        return physicalTables.getOrDefault(tableName, tableName);
    }
}

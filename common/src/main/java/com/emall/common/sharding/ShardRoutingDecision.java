package com.emall.common.sharding;

import java.util.Map;

public record ShardRoutingDecision(String logicalTable, long shardKey, int logicalShard, String cellId,
        String databaseName, int databaseIndex, Map<String, String> physicalTables) {
    public ShardRoutingDecision {
        physicalTables = Map.copyOf(physicalTables);
    }

    public String resolveTableName(String tableName) {
        return physicalTables.getOrDefault(tableName, tableName);
    }
}

package com.emall.common.sharding;

import java.util.Map;

public record PhysicalShardPlacement(String databaseName, int databaseIndex, String regionId, String cellId,
        Map<String, String> physicalTables) {
    public PhysicalShardPlacement {
        if (databaseName == null || databaseName.isBlank()) {
            throw new IllegalArgumentException("databaseName must not be blank");
        }
        if (databaseIndex < 0) {
            throw new IllegalArgumentException("databaseIndex must not be negative");
        }
        if (regionId == null || regionId.isBlank() || cellId == null || cellId.isBlank()) {
            throw new IllegalArgumentException("regionId and cellId must not be blank");
        }
        physicalTables = Map.copyOf(physicalTables);
    }

    public String resolveTableName(String logicalTable) {
        return physicalTables.getOrDefault(logicalTable, logicalTable);
    }

    public String identity(String logicalTable) {
        return databaseName + ':' + resolveTableName(logicalTable);
    }
}

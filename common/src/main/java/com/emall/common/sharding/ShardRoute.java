package com.emall.common.sharding;

public record ShardRoute(String databaseName, String tableName, int databaseIndex, int tableIndex) {
    public ShardRoute {
        if (databaseName == null || databaseName.isBlank()) {
            throw new IllegalArgumentException("databaseName must not be blank");
        }
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName must not be blank");
        }
        if (databaseIndex < 0) {
            throw new IllegalArgumentException("databaseIndex must not be negative");
        }
        if (tableIndex < 0) {
            throw new IllegalArgumentException("tableIndex must not be negative");
        }
    }
}

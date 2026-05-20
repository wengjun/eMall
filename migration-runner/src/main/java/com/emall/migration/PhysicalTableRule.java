package com.emall.migration;

public record PhysicalTableRule(String sourceTable, String tablePrefix, int tableShardCount, String cellId) {
    public PhysicalTableRule {
        if (sourceTable == null || sourceTable.isBlank()) {
            throw new IllegalArgumentException("sourceTable must not be blank");
        }
        if (tablePrefix == null || tablePrefix.isBlank()) {
            throw new IllegalArgumentException("tablePrefix must not be blank");
        }
        if (tableShardCount <= 0) {
            throw new IllegalArgumentException("tableShardCount must be positive");
        }
    }
}

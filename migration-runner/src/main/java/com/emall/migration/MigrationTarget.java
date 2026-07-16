package com.emall.migration;

import java.util.List;

public record MigrationTarget(String service, String region, int shard, String database, String jdbcUrl,
        String username, String password, List<String> locations, String historyTable, String operator, String batchId,
        boolean baselineOnMigrate, boolean dryRun, boolean createPhysicalTables, List<PhysicalTableRule> physicalTables,
        MigrationPhase phase, boolean allowDestructiveChanges, String minimumCompatibleVersion,
        String approvalReference) {
}

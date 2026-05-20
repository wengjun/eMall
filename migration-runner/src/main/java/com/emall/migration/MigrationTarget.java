package com.emall.migration;

import java.util.List;

public record MigrationTarget(String service, String region, int shard, String jdbcUrl, String username,
        String password, List<String> locations, String historyTable, String operator, boolean baselineOnMigrate,
        boolean dryRun, boolean createPhysicalTables, List<PhysicalTableRule> physicalTables) {
}

package com.emall.migration;

import java.util.ArrayList;
import java.util.List;

final class MigrationBatchPlanner {
    private MigrationBatchPlanner() {
    }

    static List<List<MigrationTarget>> plan(List<MigrationTarget> targets, int canaryCount, int batchSize) {
        if (targets.isEmpty()) {
            return List.of();
        }
        if (canaryCount <= 0 || canaryCount > targets.size() || batchSize <= 0) {
            throw new IllegalArgumentException("invalid migration batch plan");
        }
        List<List<MigrationTarget>> batches = new ArrayList<>();
        batches.add(List.copyOf(targets.subList(0, canaryCount)));
        for (int start = canaryCount; start < targets.size(); start += batchSize) {
            batches.add(List.copyOf(targets.subList(start, Math.min(start + batchSize, targets.size()))));
        }
        return List.copyOf(batches);
    }
}

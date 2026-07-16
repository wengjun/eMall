package com.emall.routing;

public record VirtualShardMigrationEvidence(String copyCursor, Long sourceRowCount, Long targetRowCount,
        String sourceChecksum, String targetChecksum, Long cdcLag) {
    public VirtualShardMigrationEvidence {
        if (sourceRowCount != null && sourceRowCount < 0 || targetRowCount != null && targetRowCount < 0
                || cdcLag != null && cdcLag < 0) {
            throw new IllegalArgumentException("migration counts and CDC lag must not be negative");
        }
    }

    public static VirtualShardMigrationEvidence empty() {
        return new VirtualShardMigrationEvidence(null, null, null, null, null, null);
    }
}

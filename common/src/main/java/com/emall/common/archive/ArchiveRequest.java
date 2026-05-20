package com.emall.common.archive;

import java.time.LocalDate;

public record ArchiveRequest(String sourceTable, String archiveTable, int shardId, LocalDate cutoffDate,
        int batchSize) {
    public ArchiveRequest {
        if (sourceTable == null || sourceTable.isBlank()) {
            throw new IllegalArgumentException("sourceTable must not be blank");
        }
        if (archiveTable == null || archiveTable.isBlank()) {
            throw new IllegalArgumentException("archiveTable must not be blank");
        }
        if (shardId < 0) {
            throw new IllegalArgumentException("shardId must not be negative");
        }
        if (cutoffDate == null) {
            throw new IllegalArgumentException("cutoffDate must not be null");
        }
        if (batchSize <= 0 || batchSize > 10000) {
            throw new IllegalArgumentException("batchSize must be between 1 and 10000");
        }
    }
}

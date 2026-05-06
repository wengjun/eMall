package com.emall.datawarehouse;

import java.time.Instant;
import java.time.LocalDate;

enum WarehouseLayer {
    ODS,
    DWD,
    DWS,
    ADS
}

enum QualityStatus {
    PASS,
    WARN,
    FAIL
}

record DatasetDefinition(long datasetId, WarehouseLayer layer, String datasetName, String owner,
                         String description, int retentionDays, Instant createdAt, Instant updatedAt) {
}

record TablePartition(long partitionId, long datasetId, String partitionKey, LocalDate partitionDate,
                      long rowCount, long storageBytes, Instant createdAt) {
}

record QualityCheck(long checkId, long datasetId, String checkName, QualityStatus status, String detail,
                    Instant checkedAt) {
}

record LineageEdge(long lineageId, long upstreamDatasetId, long downstreamDatasetId, String transformName,
                   Instant createdAt) {
}

record WarehouseSummary(int datasets, int partitions, int failedChecks, int lineageEdges) {
}

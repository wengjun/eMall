package com.emall.datawarehouse;

import com.emall.common.privacy.SensitiveDataType;
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

enum QualityAlertStatus {
    OPEN,
    ACKED
}

record DatasetDefinition(long datasetId, WarehouseLayer layer, String datasetName, String owner, String description,
        int retentionDays, Instant createdAt, Instant updatedAt) {
}

record TablePartition(long partitionId, long datasetId, String partitionKey, LocalDate partitionDate, long rowCount,
        long storageBytes, Instant createdAt) {
}

record QualityCheck(long checkId, long datasetId, String checkName, QualityStatus status, String detail,
        Instant checkedAt) {
}

record QualityAlert(long alertId, long datasetId, long checkId, String severity, String detail,
        QualityAlertStatus status, Instant createdAt, Instant updatedAt) {
}

record LineageEdge(long lineageId, long upstreamDatasetId, long downstreamDatasetId, String transformName,
        Instant createdAt) {
}

record FieldLineage(long lineageId, long upstreamDatasetId, String upstreamField, long downstreamDatasetId,
        String downstreamField, SensitiveDataType sensitivity, String transformName, Instant createdAt) {
}

record WarehouseSummary(int datasets, int partitions, int failedChecks, int lineageEdges, int fieldLineageEdges,
        int openQualityAlerts) {
}

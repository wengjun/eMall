package com.emall.datawarehouse;

import com.emall.common.privacy.SensitiveDataType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

@TableName("dataset_definition")
record DatasetDefinition(@TableId(value = "dataset_id", type = IdType.INPUT) long datasetId, WarehouseLayer layer,
        String datasetName, String owner, String description, int retentionDays, Instant createdAt,
        Instant updatedAt) {
}

@TableName("table_partition")
record TablePartition(@TableId(value = "partition_id", type = IdType.INPUT) long partitionId, long datasetId,
        String partitionKey, LocalDate partitionDate, long rowCount, long storageBytes, Instant createdAt) {
}

@TableName("quality_check")
record QualityCheck(@TableId(value = "check_id", type = IdType.INPUT) long checkId, long datasetId, String checkName,
        QualityStatus status, String detail, Instant checkedAt) {
}

@TableName("quality_alert")
record QualityAlert(@TableId(value = "alert_id", type = IdType.INPUT) long alertId, long datasetId, long checkId,
        String severity, String detail, QualityAlertStatus status, Instant createdAt, Instant updatedAt) {
}

@TableName("lineage_edge")
record LineageEdge(@TableId(value = "lineage_id", type = IdType.INPUT) long lineageId, long upstreamDatasetId,
        long downstreamDatasetId, String transformName, Instant createdAt) {
}

@TableName("field_lineage")
record FieldLineage(@TableId(value = "lineage_id", type = IdType.INPUT) long lineageId, long upstreamDatasetId,
        String upstreamField, long downstreamDatasetId, String downstreamField, SensitiveDataType sensitivity,
        String transformName, Instant createdAt) {
}

record WarehouseSummary(int datasets, int partitions, int failedChecks, int lineageEdges, int fieldLineageEdges,
        int openQualityAlerts) {
}

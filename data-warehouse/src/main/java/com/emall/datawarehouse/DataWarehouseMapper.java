package com.emall.datawarehouse;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface DataWarehouseMapper {
    @Insert("""
            INSERT INTO dataset_definition
                (dataset_id, layer, dataset_name, owner, description, retention_days, created_at, updated_at)
            VALUES (#{dataset.datasetId}, #{dataset.layer}, #{dataset.datasetName}, #{dataset.owner},
                #{dataset.description}, #{dataset.retentionDays}, #{dataset.createdAt}, #{dataset.updatedAt})
            ON DUPLICATE KEY UPDATE owner = VALUES(owner), description = VALUES(description),
                retention_days = VALUES(retention_days), updated_at = VALUES(updated_at)
            """)
    int saveDataset(@Param("dataset") DatasetDefinition dataset);

    @Select("""
            SELECT dataset_id, layer, dataset_name, owner, description, retention_days, created_at, updated_at
            FROM dataset_definition
            WHERE dataset_id = #{datasetId}
            """)
    DatasetDefinition findDataset(@Param("datasetId") long datasetId);

    @Select("""
            SELECT dataset_id, layer, dataset_name, owner, description, retention_days, created_at, updated_at
            FROM dataset_definition
            """)
    List<DatasetDefinition> findDatasets();

    @Insert("""
            INSERT INTO table_partition
                (partition_id, dataset_id, partition_key, partition_date, row_count, storage_bytes, created_at)
            VALUES (#{partition.partitionId}, #{partition.datasetId}, #{partition.partitionKey},
                #{partition.partitionDate}, #{partition.rowCount}, #{partition.storageBytes},
                #{partition.createdAt})
            """)
    int savePartition(@Param("partition") TablePartition partition);

    @Select("""
            SELECT partition_id, dataset_id, partition_key, partition_date, row_count, storage_bytes, created_at
            FROM table_partition
            WHERE dataset_id = #{datasetId}
            """)
    List<TablePartition> findPartitions(@Param("datasetId") long datasetId);

    @Insert("""
            INSERT INTO quality_check (check_id, dataset_id, check_name, status, detail, checked_at)
            VALUES (#{check.checkId}, #{check.datasetId}, #{check.checkName}, #{check.status},
                #{check.detail}, #{check.checkedAt})
            """)
    int saveQualityCheck(@Param("check") QualityCheck check);

    @Select("""
            SELECT check_id, dataset_id, check_name, status, detail, checked_at
            FROM quality_check
            """)
    List<QualityCheck> findQualityChecks();

    @Insert("""
            INSERT INTO quality_alert
                (alert_id, dataset_id, check_id, severity, detail, status, created_at, updated_at)
            VALUES (#{alert.alertId}, #{alert.datasetId}, #{alert.checkId}, #{alert.severity},
                #{alert.detail}, #{alert.status}, #{alert.createdAt}, #{alert.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveQualityAlert(@Param("alert") QualityAlert alert);

    @Select("""
            SELECT alert_id, dataset_id, check_id, severity, detail, status, created_at, updated_at
            FROM quality_alert
            """)
    List<QualityAlert> findQualityAlerts();

    @Insert("""
            INSERT INTO lineage_edge
                (lineage_id, upstream_dataset_id, downstream_dataset_id, transform_name, created_at)
            VALUES (#{edge.lineageId}, #{edge.upstreamDatasetId}, #{edge.downstreamDatasetId},
                #{edge.transformName}, #{edge.createdAt})
            """)
    int saveLineage(@Param("edge") LineageEdge edge);

    @Select("""
            SELECT lineage_id, upstream_dataset_id, downstream_dataset_id, transform_name, created_at
            FROM lineage_edge
            """)
    List<LineageEdge> findLineage();

    @Insert("""
            INSERT INTO field_lineage
                (lineage_id, upstream_dataset_id, upstream_field, downstream_dataset_id, downstream_field,
                 sensitivity, transform_name, created_at)
            VALUES (#{edge.lineageId}, #{edge.upstreamDatasetId}, #{edge.upstreamField},
                #{edge.downstreamDatasetId}, #{edge.downstreamField}, #{edge.sensitivity}, #{edge.transformName},
                #{edge.createdAt})
            """)
    int saveFieldLineage(@Param("edge") FieldLineage edge);

    @Select("""
            SELECT lineage_id, upstream_dataset_id, upstream_field, downstream_dataset_id, downstream_field,
                sensitivity, transform_name, created_at
            FROM field_lineage
            """)
    List<FieldLineage> findFieldLineage();
}

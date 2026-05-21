package com.emall.datawarehouse;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    @Insert("""
            INSERT INTO quality_alert
                (alert_id, dataset_id, check_id, severity, detail, status, created_at, updated_at)
            VALUES (#{alert.alertId}, #{alert.datasetId}, #{alert.checkId}, #{alert.severity},
                #{alert.detail}, #{alert.status}, #{alert.createdAt}, #{alert.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveQualityAlert(@Param("alert") QualityAlert alert);
}

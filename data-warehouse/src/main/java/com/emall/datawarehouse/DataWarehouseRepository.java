package com.emall.datawarehouse;

import java.util.List;
import java.util.Optional;

interface DataWarehouseRepository {
    DatasetDefinition saveDataset(DatasetDefinition dataset);

    Optional<DatasetDefinition> findDataset(long datasetId);

    List<DatasetDefinition> findDatasets();

    TablePartition savePartition(TablePartition partition);

    List<TablePartition> findPartitions(long datasetId);

    QualityCheck saveQualityCheck(QualityCheck check);

    List<QualityCheck> findQualityChecks();

    QualityAlert saveQualityAlert(QualityAlert alert);

    List<QualityAlert> findQualityAlerts();

    LineageEdge saveLineage(LineageEdge lineage);

    List<LineageEdge> findLineage();

    FieldLineage saveFieldLineage(FieldLineage lineage);

    List<FieldLineage> findFieldLineage();
}

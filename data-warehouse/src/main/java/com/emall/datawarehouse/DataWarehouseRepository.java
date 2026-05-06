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

    LineageEdge saveLineage(LineageEdge lineage);

    List<LineageEdge> findLineage();
}

package com.emall.datawarehouse;

import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.localDateValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusDataWarehouseRepository implements DataWarehouseRepository {
    private final DataWarehouseMapper dataWarehouseMapper;

    MybatisPlusDataWarehouseRepository(DataWarehouseMapper dataWarehouseMapper) {
        this.dataWarehouseMapper = dataWarehouseMapper;
    }

    @Override
    public DatasetDefinition saveDataset(DatasetDefinition dataset) {
        dataWarehouseMapper.saveDataset(dataset);
        return dataset;
    }

    @Override
    public Optional<DatasetDefinition> findDataset(long datasetId) {
        return Optional.ofNullable(dataWarehouseMapper.findDataset(datasetId)).map(this::mapDataset);
    }

    @Override
    public List<DatasetDefinition> findDatasets() {
        return dataWarehouseMapper.findDatasets().stream().map(this::mapDataset).toList();
    }

    @Override
    public TablePartition savePartition(TablePartition partition) {
        dataWarehouseMapper.savePartition(partition);
        return partition;
    }

    @Override
    public List<TablePartition> findPartitions(long datasetId) {
        return dataWarehouseMapper.findPartitions(datasetId).stream().map(this::mapPartition).toList();
    }

    @Override
    public QualityCheck saveQualityCheck(QualityCheck check) {
        dataWarehouseMapper.saveQualityCheck(check);
        return check;
    }

    @Override
    public List<QualityCheck> findQualityChecks() {
        return dataWarehouseMapper.findQualityChecks().stream().map(this::mapQualityCheck).toList();
    }

    @Override
    public LineageEdge saveLineage(LineageEdge edge) {
        dataWarehouseMapper.saveLineage(edge);
        return edge;
    }

    @Override
    public List<LineageEdge> findLineage() {
        return dataWarehouseMapper.findLineage().stream().map(this::mapLineage).toList();
    }

    private DatasetDefinition mapDataset(Map<String, Object> row) {
        return new DatasetDefinition(longValue(row, "dataset_id"), WarehouseLayer.valueOf(stringValue(row, "layer")),
                stringValue(row, "dataset_name"), stringValue(row, "owner"), stringValue(row, "description"),
                intValue(row, "retention_days"), instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private TablePartition mapPartition(Map<String, Object> row) {
        return new TablePartition(longValue(row, "partition_id"), longValue(row, "dataset_id"),
                stringValue(row, "partition_key"), localDateValue(row, "partition_date"),
                longValue(row, "row_count"), longValue(row, "storage_bytes"), instantValue(row, "created_at"));
    }

    private QualityCheck mapQualityCheck(Map<String, Object> row) {
        return new QualityCheck(longValue(row, "check_id"), longValue(row, "dataset_id"),
                stringValue(row, "check_name"), QualityStatus.valueOf(stringValue(row, "status")),
                stringValue(row, "detail"), instantValue(row, "checked_at"));
    }

    private LineageEdge mapLineage(Map<String, Object> row) {
        return new LineageEdge(longValue(row, "lineage_id"), longValue(row, "upstream_dataset_id"),
                longValue(row, "downstream_dataset_id"), stringValue(row, "transform_name"),
                instantValue(row, "created_at"));
    }
}

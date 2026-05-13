package com.emall.datawarehouse;

import java.util.List;
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
        return Optional.ofNullable(dataWarehouseMapper.findDataset(datasetId));
    }

    @Override
    public List<DatasetDefinition> findDatasets() {
        return dataWarehouseMapper.findDatasets();
    }

    @Override
    public TablePartition savePartition(TablePartition partition) {
        dataWarehouseMapper.savePartition(partition);
        return partition;
    }

    @Override
    public List<TablePartition> findPartitions(long datasetId) {
        return dataWarehouseMapper.findPartitions(datasetId);
    }

    @Override
    public QualityCheck saveQualityCheck(QualityCheck check) {
        dataWarehouseMapper.saveQualityCheck(check);
        return check;
    }

    @Override
    public List<QualityCheck> findQualityChecks() {
        return dataWarehouseMapper.findQualityChecks();
    }

    @Override
    public LineageEdge saveLineage(LineageEdge edge) {
        dataWarehouseMapper.saveLineage(edge);
        return edge;
    }

    @Override
    public List<LineageEdge> findLineage() {
        return dataWarehouseMapper.findLineage();
    }
}

package com.emall.datawarehouse;

import java.util.List;
import java.util.Optional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusDataWarehouseRepository implements DataWarehouseRepository {
    private final DataWarehouseMapper dataWarehouseMapper;
    private final DatasetDefinitionMapper datasetMapper;
    private final TablePartitionMapper partitionMapper;
    private final QualityCheckMapper qualityCheckMapper;
    private final QualityAlertMapper qualityAlertMapper;
    private final LineageEdgeMapper lineageMapper;
    private final FieldLineageMapper fieldLineageMapper;

    MybatisPlusDataWarehouseRepository(DataWarehouseMapper dataWarehouseMapper, DatasetDefinitionMapper datasetMapper,
            TablePartitionMapper partitionMapper, QualityCheckMapper qualityCheckMapper,
            QualityAlertMapper qualityAlertMapper, LineageEdgeMapper lineageMapper,
            FieldLineageMapper fieldLineageMapper) {
        this.dataWarehouseMapper = dataWarehouseMapper;
        this.datasetMapper = datasetMapper;
        this.partitionMapper = partitionMapper;
        this.qualityCheckMapper = qualityCheckMapper;
        this.qualityAlertMapper = qualityAlertMapper;
        this.lineageMapper = lineageMapper;
        this.fieldLineageMapper = fieldLineageMapper;
    }

    @Override
    public DatasetDefinition saveDataset(DatasetDefinition dataset) {
        dataWarehouseMapper.saveDataset(dataset);
        return dataset;
    }

    @Override
    public Optional<DatasetDefinition> findDataset(long datasetId) {
        return Optional.ofNullable(datasetMapper.selectById(datasetId));
    }

    @Override
    public List<DatasetDefinition> findDatasets() {
        return datasetMapper.selectList(null);
    }

    @Override
    public TablePartition savePartition(TablePartition partition) {
        partitionMapper.insert(partition);
        return partition;
    }

    @Override
    public List<TablePartition> findPartitions(long datasetId) {
        return partitionMapper.selectList(new QueryWrapper<TablePartition>().eq("dataset_id", datasetId));
    }

    @Override
    public QualityCheck saveQualityCheck(QualityCheck check) {
        qualityCheckMapper.insert(check);
        return check;
    }

    @Override
    public List<QualityCheck> findQualityChecks() {
        return qualityCheckMapper.selectList(null);
    }

    @Override
    public QualityAlert saveQualityAlert(QualityAlert alert) {
        dataWarehouseMapper.saveQualityAlert(alert);
        return alert;
    }

    @Override
    public List<QualityAlert> findQualityAlerts() {
        return qualityAlertMapper.selectList(null);
    }

    @Override
    public LineageEdge saveLineage(LineageEdge edge) {
        lineageMapper.insert(edge);
        return edge;
    }

    @Override
    public List<LineageEdge> findLineage() {
        return lineageMapper.selectList(null);
    }

    @Override
    public FieldLineage saveFieldLineage(FieldLineage lineage) {
        fieldLineageMapper.insert(lineage);
        return lineage;
    }

    @Override
    public List<FieldLineage> findFieldLineage() {
        return fieldLineageMapper.selectList(null);
    }
}

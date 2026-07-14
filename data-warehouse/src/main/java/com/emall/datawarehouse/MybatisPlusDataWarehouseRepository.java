package com.emall.datawarehouse;

import java.util.List;
import java.util.Optional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.common.persistence.BoundedQuery;
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
        return BoundedQuery.firstPage(datasetMapper);
    }

    @Override
    public TablePartition savePartition(TablePartition partition) {
        partitionMapper.insert(partition);
        return partition;
    }

    @Override
    public List<TablePartition> findPartitions(long datasetId) {
        return BoundedQuery.firstPage(partitionMapper, new QueryWrapper<TablePartition>().eq("dataset_id", datasetId));
    }

    @Override
    public QualityCheck saveQualityCheck(QualityCheck check) {
        qualityCheckMapper.insert(check);
        return check;
    }

    @Override
    public List<QualityCheck> findQualityChecks() {
        return BoundedQuery.firstPage(qualityCheckMapper);
    }

    @Override
    public QualityAlert saveQualityAlert(QualityAlert alert) {
        dataWarehouseMapper.saveQualityAlert(alert);
        return alert;
    }

    @Override
    public List<QualityAlert> findQualityAlerts() {
        return BoundedQuery.firstPage(qualityAlertMapper);
    }

    @Override
    public LineageEdge saveLineage(LineageEdge edge) {
        lineageMapper.insert(edge);
        return edge;
    }

    @Override
    public List<LineageEdge> findLineage() {
        return BoundedQuery.firstPage(lineageMapper);
    }

    @Override
    public FieldLineage saveFieldLineage(FieldLineage lineage) {
        fieldLineageMapper.insert(lineage);
        return lineage;
    }

    @Override
    public List<FieldLineage> findFieldLineage() {
        return BoundedQuery.firstPage(fieldLineageMapper);
    }
}

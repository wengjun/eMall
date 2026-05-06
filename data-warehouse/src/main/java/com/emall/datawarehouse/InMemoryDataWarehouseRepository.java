package com.emall.datawarehouse;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryDataWarehouseRepository implements DataWarehouseRepository {
    private final ConcurrentMap<Long, DatasetDefinition> datasets = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, TablePartition> partitions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, QualityCheck> checks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, LineageEdge> lineage = new ConcurrentHashMap<>();

    @Override
    public DatasetDefinition saveDataset(DatasetDefinition dataset) {
        datasets.put(dataset.datasetId(), dataset);
        return dataset;
    }

    @Override
    public Optional<DatasetDefinition> findDataset(long datasetId) {
        return Optional.ofNullable(datasets.get(datasetId));
    }

    @Override
    public List<DatasetDefinition> findDatasets() {
        return List.copyOf(datasets.values());
    }

    @Override
    public TablePartition savePartition(TablePartition partition) {
        partitions.put(partition.partitionId(), partition);
        return partition;
    }

    @Override
    public List<TablePartition> findPartitions(long datasetId) {
        return partitions.values().stream().filter(partition -> partition.datasetId() == datasetId).toList();
    }

    @Override
    public QualityCheck saveQualityCheck(QualityCheck check) {
        checks.put(check.checkId(), check);
        return check;
    }

    @Override
    public List<QualityCheck> findQualityChecks() {
        return List.copyOf(checks.values());
    }

    @Override
    public LineageEdge saveLineage(LineageEdge edge) {
        lineage.put(edge.lineageId(), edge);
        return edge;
    }

    @Override
    public List<LineageEdge> findLineage() {
        return List.copyOf(lineage.values());
    }
}

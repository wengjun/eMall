package com.emall.datawarehouse;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcDataWarehouseRepository implements DataWarehouseRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcDataWarehouseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DatasetDefinition saveDataset(DatasetDefinition dataset) {
        jdbcTemplate.update("""
                INSERT INTO dataset_definition
                    (dataset_id, layer, dataset_name, owner, description, retention_days, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE owner = VALUES(owner), description = VALUES(description),
                    retention_days = VALUES(retention_days), updated_at = VALUES(updated_at)
                """, dataset.datasetId(), dataset.layer().name(), dataset.datasetName(), dataset.owner(),
                dataset.description(), dataset.retentionDays(), Timestamp.from(dataset.createdAt()),
                Timestamp.from(dataset.updatedAt()));
        return dataset;
    }

    @Override
    public Optional<DatasetDefinition> findDataset(long datasetId) {
        return jdbcTemplate.query("SELECT * FROM dataset_definition WHERE dataset_id = ?", this::mapDataset, datasetId)
                .stream().findFirst();
    }

    @Override
    public List<DatasetDefinition> findDatasets() {
        return jdbcTemplate.query("SELECT * FROM dataset_definition", this::mapDataset);
    }

    @Override
    public TablePartition savePartition(TablePartition partition) {
        jdbcTemplate.update("""
                INSERT INTO table_partition
                    (partition_id, dataset_id, partition_key, partition_date, row_count, storage_bytes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, partition.partitionId(), partition.datasetId(), partition.partitionKey(),
                Date.valueOf(partition.partitionDate()), partition.rowCount(), partition.storageBytes(),
                Timestamp.from(partition.createdAt()));
        return partition;
    }

    @Override
    public List<TablePartition> findPartitions(long datasetId) {
        return jdbcTemplate.query("SELECT * FROM table_partition WHERE dataset_id = ?", this::mapPartition, datasetId);
    }

    @Override
    public QualityCheck saveQualityCheck(QualityCheck check) {
        jdbcTemplate.update("""
                INSERT INTO quality_check (check_id, dataset_id, check_name, status, detail, checked_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, check.checkId(), check.datasetId(), check.checkName(), check.status().name(), check.detail(),
                Timestamp.from(check.checkedAt()));
        return check;
    }

    @Override
    public List<QualityCheck> findQualityChecks() {
        return jdbcTemplate.query("SELECT * FROM quality_check", this::mapQualityCheck);
    }

    @Override
    public LineageEdge saveLineage(LineageEdge edge) {
        jdbcTemplate.update("""
                INSERT INTO lineage_edge
                    (lineage_id, upstream_dataset_id, downstream_dataset_id, transform_name, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, edge.lineageId(), edge.upstreamDatasetId(), edge.downstreamDatasetId(), edge.transformName(),
                Timestamp.from(edge.createdAt()));
        return edge;
    }

    @Override
    public List<LineageEdge> findLineage() {
        return jdbcTemplate.query("SELECT * FROM lineage_edge", this::mapLineage);
    }

    private DatasetDefinition mapDataset(ResultSet rs, int rowNum) throws SQLException {
        return new DatasetDefinition(rs.getLong("dataset_id"), WarehouseLayer.valueOf(rs.getString("layer")),
                rs.getString("dataset_name"), rs.getString("owner"), rs.getString("description"),
                rs.getInt("retention_days"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private TablePartition mapPartition(ResultSet rs, int rowNum) throws SQLException {
        return new TablePartition(rs.getLong("partition_id"), rs.getLong("dataset_id"), rs.getString("partition_key"),
                rs.getDate("partition_date").toLocalDate(), rs.getLong("row_count"), rs.getLong("storage_bytes"),
                rs.getTimestamp("created_at").toInstant());
    }

    private QualityCheck mapQualityCheck(ResultSet rs, int rowNum) throws SQLException {
        return new QualityCheck(rs.getLong("check_id"), rs.getLong("dataset_id"), rs.getString("check_name"),
                QualityStatus.valueOf(rs.getString("status")), rs.getString("detail"),
                rs.getTimestamp("checked_at").toInstant());
    }

    private LineageEdge mapLineage(ResultSet rs, int rowNum) throws SQLException {
        return new LineageEdge(rs.getLong("lineage_id"), rs.getLong("upstream_dataset_id"),
                rs.getLong("downstream_dataset_id"), rs.getString("transform_name"),
                rs.getTimestamp("created_at").toInstant());
    }
}

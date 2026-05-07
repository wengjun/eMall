package com.emall.datawarehouse;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DataWarehouseService {
    private final DataWarehouseRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    DataWarehouseService(DataWarehouseRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    DatasetDefinition registerDataset(WarehouseLayer layer, String datasetName, String owner, String description,
            int retentionDays) {
        if (retentionDays <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "retention days must be positive");
        }
        Instant now = Instant.now();
        return repository.saveDataset(new DatasetDefinition(idGenerator.nextId(), layer, normalize(datasetName),
                normalize(owner), description, retentionDays, now, now));
    }

    @Transactional
    TablePartition addPartition(long datasetId, String partitionKey, LocalDate partitionDate, long rowCount,
            long storageBytes) {
        requireDataset(datasetId);
        return repository.savePartition(new TablePartition(idGenerator.nextId(), datasetId, normalize(partitionKey),
                partitionDate, Math.max(0, rowCount), Math.max(0, storageBytes), Instant.now()));
    }

    @Transactional
    QualityCheck recordQualityCheck(long datasetId, String checkName, QualityStatus status, String detail) {
        requireDataset(datasetId);
        return repository.saveQualityCheck(
                new QualityCheck(idGenerator.nextId(), datasetId, normalize(checkName), status, detail, Instant.now()));
    }

    @Transactional
    LineageEdge addLineage(long upstreamDatasetId, long downstreamDatasetId, String transformName) {
        requireDataset(upstreamDatasetId);
        requireDataset(downstreamDatasetId);
        return repository.saveLineage(new LineageEdge(idGenerator.nextId(), upstreamDatasetId, downstreamDatasetId,
                normalize(transformName), Instant.now()));
    }

    WarehouseSummary summary() {
        int failedChecks = (int) repository.findQualityChecks().stream()
                .filter(check -> check.status() == QualityStatus.FAIL).count();
        int partitions = repository.findDatasets().stream()
                .mapToInt(dataset -> repository.findPartitions(dataset.datasetId()).size()).sum();
        return new WarehouseSummary(repository.findDatasets().size(), partitions, failedChecks,
                repository.findLineage().size());
    }

    private DatasetDefinition requireDataset(long datasetId) {
        return repository.findDataset(datasetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "warehouse dataset not found"));
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "warehouse value must not be blank");
        }
        return normalized;
    }
}

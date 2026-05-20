package com.emall.datawarehouse;

import com.emall.common.api.ApiResponse;
import com.emall.common.privacy.SensitiveDataType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-warehouse")
class DataWarehouseController {
    private final DataWarehouseService dataWarehouseService;

    DataWarehouseController(DataWarehouseService dataWarehouseService) {
        this.dataWarehouseService = dataWarehouseService;
    }

    @PostMapping("/datasets")
    ApiResponse<DatasetDefinition> registerDataset(@Valid @RequestBody RegisterDatasetRequest request) {
        return ApiResponse.ok(dataWarehouseService.registerDataset(request.layer(), request.datasetName(),
                request.owner(), request.description(), request.retentionDays()));
    }

    @PostMapping("/partitions")
    ApiResponse<TablePartition> addPartition(@Valid @RequestBody AddPartitionRequest request) {
        return ApiResponse.ok(dataWarehouseService.addPartition(request.datasetId(), request.partitionKey(),
                request.partitionDate(), request.rowCount(), request.storageBytes()));
    }

    @PostMapping("/quality-checks")
    ApiResponse<QualityCheck> recordQualityCheck(@Valid @RequestBody RecordQualityCheckRequest request) {
        return ApiResponse.ok(dataWarehouseService.recordQualityCheck(request.datasetId(), request.checkName(),
                request.status(), request.detail()));
    }

    @PostMapping("/lineage")
    ApiResponse<LineageEdge> addLineage(@Valid @RequestBody AddLineageRequest request) {
        return ApiResponse.ok(dataWarehouseService.addLineage(request.upstreamDatasetId(),
                request.downstreamDatasetId(), request.transformName()));
    }

    @PostMapping("/field-lineage")
    ApiResponse<FieldLineage> addFieldLineage(@Valid @RequestBody AddFieldLineageRequest request) {
        return ApiResponse.ok(dataWarehouseService.addFieldLineage(request.upstreamDatasetId(), request.upstreamField(),
                request.downstreamDatasetId(), request.downstreamField(), request.sensitivity(),
                request.transformName()));
    }

    @GetMapping("/summary")
    ApiResponse<WarehouseSummary> summary() {
        return ApiResponse.ok(dataWarehouseService.summary());
    }

    record RegisterDatasetRequest(WarehouseLayer layer, @NotBlank String datasetName, @NotBlank String owner,
            @NotBlank String description, @Positive int retentionDays) {
    }

    record AddPartitionRequest(@Positive long datasetId, @NotBlank String partitionKey, LocalDate partitionDate,
            @Min(0) long rowCount, @Min(0) long storageBytes) {
    }

    record RecordQualityCheckRequest(@Positive long datasetId, @NotBlank String checkName, QualityStatus status,
            @NotBlank String detail) {
    }

    record AddLineageRequest(@Positive long upstreamDatasetId, @Positive long downstreamDatasetId,
            @NotBlank String transformName) {
    }

    record AddFieldLineageRequest(@Positive long upstreamDatasetId, @NotBlank String upstreamField,
            @Positive long downstreamDatasetId, @NotBlank String downstreamField, SensitiveDataType sensitivity,
            @NotBlank String transformName) {
    }
}

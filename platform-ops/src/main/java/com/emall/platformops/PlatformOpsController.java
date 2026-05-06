package com.emall.platformops;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform-ops")
class PlatformOpsController {
    private final PlatformOpsService platformOpsService;

    PlatformOpsController(PlatformOpsService platformOpsService) {
        this.platformOpsService = platformOpsService;
    }

    @PostMapping("/backups")
    ApiResponse<BackupPlan> createBackupPlan(@Valid @RequestBody CreateBackupPlanRequest request) {
        return ApiResponse.ok(platformOpsService.createBackupPlan(request.databaseName(), request.backupType(),
                request.retentionDays()));
    }

    @PatchMapping("/backups/{planId}/status")
    ApiResponse<BackupPlan> changeBackupStatus(@PathVariable long planId,
                                               @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.ok(platformOpsService.changeBackupStatus(planId, request.status()));
    }

    @PostMapping("/database-operations")
    ApiResponse<DatabaseOperation> createDatabaseOperation(
            @Valid @RequestBody CreateDatabaseOperationRequest request) {
        return ApiResponse.ok(platformOpsService.createDatabaseOperation(request.databaseName(),
                request.operationType(), request.riskLevel(), request.detail()));
    }

    @PatchMapping("/database-operations/{operationId}/status")
    ApiResponse<DatabaseOperation> changeDatabaseOperationStatus(@PathVariable long operationId,
                                                                 @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.ok(platformOpsService.changeDatabaseOperationStatus(operationId, request.status()));
    }

    @PostMapping("/finops-actions")
    ApiResponse<FinOpsAction> createFinOpsAction(@Valid @RequestBody CreateFinOpsActionRequest request) {
        return ApiResponse.ok(platformOpsService.createFinOpsAction(request.serviceName(), request.actionType(),
                request.estimatedSaving()));
    }

    @PatchMapping("/finops-actions/{actionId}/approve")
    ApiResponse<FinOpsAction> approveFinOpsAction(@PathVariable long actionId) {
        return ApiResponse.ok(platformOpsService.approveFinOpsAction(actionId));
    }

    @PostMapping("/security-operations")
    ApiResponse<SecurityOperation> createSecurityOperation(
            @Valid @RequestBody CreateSecurityOperationRequest request) {
        return ApiResponse.ok(platformOpsService.createSecurityOperation(request.serviceName(), request.signalType(),
                request.riskLevel()));
    }

    @PatchMapping("/security-operations/{operationId}/status")
    ApiResponse<SecurityOperation> changeSecurityStatus(@PathVariable long operationId,
                                                        @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.ok(platformOpsService.changeSecurityStatus(operationId, request.status()));
    }

    @GetMapping("/summary")
    ApiResponse<PlatformOpsSummary> summary() {
        return ApiResponse.ok(platformOpsService.summary());
    }

    record CreateBackupPlanRequest(@NotBlank String databaseName, @NotBlank String backupType,
                                   @Positive int retentionDays) {
    }

    record ChangeStatusRequest(OpsStatus status) {
    }

    record CreateDatabaseOperationRequest(@NotBlank String databaseName, @NotBlank String operationType,
                                          RiskLevel riskLevel, @NotBlank String detail) {
    }

    record CreateFinOpsActionRequest(@NotBlank String serviceName, @NotBlank String actionType,
                                     @PositiveOrZero BigDecimal estimatedSaving) {
    }

    record CreateSecurityOperationRequest(@NotBlank String serviceName, @NotBlank String signalType,
                                          RiskLevel riskLevel) {
    }
}

package com.emall.operations;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
class OperationsController {
    private final OperationsService operationsService;

    OperationsController(OperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @PostMapping("/approvals")
    ApiResponse<ApprovalRequest> createApproval(@Valid @RequestBody CreateApprovalRequest request) {
        return ApiResponse.ok(operationsService.createApproval(request.workflowType(), request.resourceType(),
                request.resourceId(), request.requester(), request.reason()));
    }

    @PatchMapping("/approvals/{approvalId}/decision")
    ApiResponse<ApprovalRequest> decideApproval(@PathVariable long approvalId,
            @Valid @RequestBody DecideApprovalRequest request) {
        return ApiResponse.ok(operationsService.decideApproval(approvalId, request.operator(), request.status()));
    }

    @GetMapping("/approvals")
    ApiResponse<List<ApprovalRequest>> findApprovals(@RequestParam(defaultValue = "PENDING") ApprovalStatus status) {
        return ApiResponse.ok(operationsService.findApprovals(status));
    }

    @PostMapping("/tasks")
    ApiResponse<OperationTask> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.ok(operationsService.createTask(request.taskType(), request.resourceType(),
                request.resourceId(), request.owner(), request.priority(), request.summary()));
    }

    @PatchMapping("/tasks/{taskId}/status")
    ApiResponse<OperationTask> changeTaskStatus(@PathVariable long taskId,
            @Valid @RequestBody ChangeTaskStatusRequest request) {
        return ApiResponse.ok(operationsService.changeTaskStatus(taskId, request.status()));
    }

    @GetMapping("/tasks")
    ApiResponse<List<OperationTask>> findTasks(@RequestParam(defaultValue = "OPEN") TaskStatus status) {
        return ApiResponse.ok(operationsService.findTasks(status));
    }

    @PostMapping("/evidence")
    ApiResponse<ComplianceEvidence> recordEvidence(@Valid @RequestBody RecordEvidenceRequest request) {
        return ApiResponse.ok(operationsService.recordEvidence(request.evidenceType(), request.resourceType(),
                request.resourceId(), request.owner(), request.summary()));
    }

    @GetMapping("/evidence")
    ApiResponse<List<ComplianceEvidence>> findEvidence(@RequestParam String resourceType,
            @RequestParam String resourceId) {
        return ApiResponse.ok(operationsService.findEvidence(resourceType, resourceId));
    }

    @PostMapping("/security-incidents")
    ApiResponse<SecurityIncident> openIncident(@Valid @RequestBody OpenIncidentRequest request) {
        return ApiResponse.ok(operationsService.openIncident(request.severity(), request.owner(), request.summary()));
    }

    @PatchMapping("/security-incidents/{incidentId}/status")
    ApiResponse<SecurityIncident> changeIncidentStatus(@PathVariable long incidentId,
            @Valid @RequestBody ChangeIncidentStatusRequest request) {
        return ApiResponse.ok(operationsService.changeIncidentStatus(incidentId, request.status()));
    }

    record CreateApprovalRequest(@NotBlank String workflowType, @NotBlank String resourceType,
            @NotBlank String resourceId, @NotBlank String requester, @NotBlank String reason) {
    }

    record DecideApprovalRequest(@NotBlank String operator, @NotNull ApprovalStatus status) {
    }

    record CreateTaskRequest(@NotBlank String taskType, @NotBlank String resourceType, @NotBlank String resourceId,
            @NotBlank String owner, @Min(1) @Max(5) int priority, @NotBlank String summary) {
    }

    record ChangeTaskStatusRequest(@NotNull TaskStatus status) {
    }

    record RecordEvidenceRequest(@NotBlank String evidenceType, @NotBlank String resourceType,
            @NotBlank String resourceId, @NotBlank String owner, @NotBlank String summary) {
    }

    record OpenIncidentRequest(@NotBlank String severity, @NotBlank String owner, @NotBlank String summary) {
    }

    record ChangeIncidentStatusRequest(@NotNull IncidentStatus status) {
    }
}

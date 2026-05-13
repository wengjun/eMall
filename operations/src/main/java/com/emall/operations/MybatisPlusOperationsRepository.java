package com.emall.operations;

import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusOperationsRepository implements OperationsRepository {
    private final OperationsMapper operationsMapper;

    MybatisPlusOperationsRepository(OperationsMapper operationsMapper) {
        this.operationsMapper = operationsMapper;
    }

    @Override
    public ApprovalRequest saveApproval(ApprovalRequest approval) {
        operationsMapper.saveApproval(approval);
        return approval;
    }

    @Override
    public Optional<ApprovalRequest> findApproval(long approvalId) {
        return Optional.ofNullable(operationsMapper.findApproval(approvalId)).map(this::mapApproval);
    }

    @Override
    public List<ApprovalRequest> findApprovals(ApprovalStatus status) {
        return operationsMapper.findApprovals(status).stream().map(this::mapApproval).toList();
    }

    @Override
    public OperationTask saveTask(OperationTask task) {
        operationsMapper.saveTask(task);
        return task;
    }

    @Override
    public Optional<OperationTask> findTask(long taskId) {
        return Optional.ofNullable(operationsMapper.findTask(taskId)).map(this::mapTask);
    }

    @Override
    public List<OperationTask> findTasks(TaskStatus status) {
        return operationsMapper.findTasks(status).stream().map(this::mapTask).toList();
    }

    @Override
    public ComplianceEvidence saveEvidence(ComplianceEvidence evidence) {
        operationsMapper.saveEvidence(evidence);
        return evidence;
    }

    @Override
    public List<ComplianceEvidence> findEvidence(String resourceType, String resourceId) {
        return operationsMapper.findEvidence(resourceType, resourceId).stream().map(this::mapEvidence).toList();
    }

    @Override
    public SecurityIncident saveIncident(SecurityIncident incident) {
        operationsMapper.saveIncident(incident);
        return incident;
    }

    @Override
    public Optional<SecurityIncident> findIncident(long incidentId) {
        return Optional.ofNullable(operationsMapper.findIncident(incidentId)).map(this::mapIncident);
    }

    private ApprovalRequest mapApproval(Map<String, Object> row) {
        return new ApprovalRequest(longValue(row, "approval_id"), stringValue(row, "workflow_type"),
                stringValue(row, "resource_type"), stringValue(row, "resource_id"), stringValue(row, "requester"),
                stringValue(row, "approver"), stringValue(row, "reason"),
                ApprovalStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private OperationTask mapTask(Map<String, Object> row) {
        return new OperationTask(longValue(row, "task_id"), stringValue(row, "task_type"),
                stringValue(row, "resource_type"), stringValue(row, "resource_id"), stringValue(row, "owner"),
                TaskStatus.valueOf(stringValue(row, "status")), intValue(row, "priority"),
                stringValue(row, "summary"), instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private ComplianceEvidence mapEvidence(Map<String, Object> row) {
        return new ComplianceEvidence(longValue(row, "evidence_id"), stringValue(row, "evidence_type"),
                stringValue(row, "resource_type"), stringValue(row, "resource_id"), stringValue(row, "owner"),
                stringValue(row, "summary"), instantValue(row, "created_at"));
    }

    private SecurityIncident mapIncident(Map<String, Object> row) {
        return new SecurityIncident(longValue(row, "incident_id"), stringValue(row, "severity"),
                stringValue(row, "owner"), stringValue(row, "summary"),
                IncidentStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }
}

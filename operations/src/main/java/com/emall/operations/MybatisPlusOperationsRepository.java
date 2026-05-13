package com.emall.operations;

import java.util.List;
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
        return Optional.ofNullable(operationsMapper.findApproval(approvalId));
    }

    @Override
    public List<ApprovalRequest> findApprovals(ApprovalStatus status) {
        return operationsMapper.findApprovals(status);
    }

    @Override
    public OperationTask saveTask(OperationTask task) {
        operationsMapper.saveTask(task);
        return task;
    }

    @Override
    public Optional<OperationTask> findTask(long taskId) {
        return Optional.ofNullable(operationsMapper.findTask(taskId));
    }

    @Override
    public List<OperationTask> findTasks(TaskStatus status) {
        return operationsMapper.findTasks(status);
    }

    @Override
    public ComplianceEvidence saveEvidence(ComplianceEvidence evidence) {
        operationsMapper.saveEvidence(evidence);
        return evidence;
    }

    @Override
    public List<ComplianceEvidence> findEvidence(String resourceType, String resourceId) {
        return operationsMapper.findEvidence(resourceType, resourceId);
    }

    @Override
    public SecurityIncident saveIncident(SecurityIncident incident) {
        operationsMapper.saveIncident(incident);
        return incident;
    }

    @Override
    public Optional<SecurityIncident> findIncident(long incidentId) {
        return Optional.ofNullable(operationsMapper.findIncident(incidentId));
    }
}

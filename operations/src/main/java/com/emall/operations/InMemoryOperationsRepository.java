package com.emall.operations;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryOperationsRepository implements OperationsRepository {
    private final ConcurrentMap<Long, ApprovalRequest> approvals = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, OperationTask> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ComplianceEvidence> evidence = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, SecurityIncident> incidents = new ConcurrentHashMap<>();

    @Override
    public ApprovalRequest saveApproval(ApprovalRequest approval) {
        approvals.put(approval.approvalId(), approval);
        return approval;
    }

    @Override
    public Optional<ApprovalRequest> findApproval(long approvalId) {
        return Optional.ofNullable(approvals.get(approvalId));
    }

    @Override
    public List<ApprovalRequest> findApprovals(ApprovalStatus status) {
        return approvals.values().stream().filter(approval -> approval.status() == status)
                .sorted(Comparator.comparing(ApprovalRequest::updatedAt).reversed()).toList();
    }

    @Override
    public OperationTask saveTask(OperationTask task) {
        tasks.put(task.taskId(), task);
        return task;
    }

    @Override
    public Optional<OperationTask> findTask(long taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    @Override
    public List<OperationTask> findTasks(TaskStatus status) {
        return tasks.values().stream().filter(task -> task.status() == status)
                .sorted(Comparator.comparingInt(OperationTask::priority)
                        .thenComparing(Comparator.comparing(OperationTask::updatedAt).reversed()))
                .toList();
    }

    @Override
    public ComplianceEvidence saveEvidence(ComplianceEvidence item) {
        evidence.put(item.evidenceId(), item);
        return item;
    }

    @Override
    public List<ComplianceEvidence> findEvidence(String resourceType, String resourceId) {
        return evidence.values().stream().filter(item -> item.resourceType().equals(resourceType))
                .filter(item -> item.resourceId().equals(resourceId))
                .sorted(Comparator.comparing(ComplianceEvidence::createdAt).reversed()).toList();
    }

    @Override
    public SecurityIncident saveIncident(SecurityIncident incident) {
        incidents.put(incident.incidentId(), incident);
        return incident;
    }

    @Override
    public Optional<SecurityIncident> findIncident(long incidentId) {
        return Optional.ofNullable(incidents.get(incidentId));
    }
}

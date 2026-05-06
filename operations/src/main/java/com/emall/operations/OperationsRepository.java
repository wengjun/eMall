package com.emall.operations;

import java.util.List;
import java.util.Optional;

interface OperationsRepository {
    ApprovalRequest saveApproval(ApprovalRequest approval);

    Optional<ApprovalRequest> findApproval(long approvalId);

    List<ApprovalRequest> findApprovals(ApprovalStatus status);

    OperationTask saveTask(OperationTask task);

    Optional<OperationTask> findTask(long taskId);

    List<OperationTask> findTasks(TaskStatus status);

    ComplianceEvidence saveEvidence(ComplianceEvidence evidence);

    List<ComplianceEvidence> findEvidence(String resourceType, String resourceId);

    SecurityIncident saveIncident(SecurityIncident incident);

    Optional<SecurityIncident> findIncident(long incidentId);
}

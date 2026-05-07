package com.emall.operations;

import java.time.Instant;

enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}

enum TaskStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED
}

enum IncidentStatus {
    OPEN,
    MITIGATED,
    CLOSED
}

record ApprovalRequest(long approvalId, String workflowType, String resourceType, String resourceId, String requester,
        String approver, String reason, ApprovalStatus status, Instant createdAt, Instant updatedAt) {
    ApprovalRequest decide(String operator, ApprovalStatus nextStatus) {
        return new ApprovalRequest(approvalId, workflowType, resourceType, resourceId, requester, operator, reason,
                nextStatus, createdAt, Instant.now());
    }
}

record OperationTask(long taskId, String taskType, String resourceType, String resourceId, String owner,
        TaskStatus status, int priority, String summary, Instant createdAt, Instant updatedAt) {
    OperationTask changeStatus(TaskStatus nextStatus) {
        return new OperationTask(taskId, taskType, resourceType, resourceId, owner, nextStatus, priority, summary,
                createdAt, Instant.now());
    }
}

record ComplianceEvidence(long evidenceId, String evidenceType, String resourceType, String resourceId, String owner,
        String summary, Instant createdAt) {
}

record SecurityIncident(long incidentId, String severity, String owner, String summary, IncidentStatus status,
        Instant createdAt, Instant updatedAt) {
    SecurityIncident changeStatus(IncidentStatus nextStatus) {
        return new SecurityIncident(incidentId, severity, owner, summary, nextStatus, createdAt, Instant.now());
    }
}

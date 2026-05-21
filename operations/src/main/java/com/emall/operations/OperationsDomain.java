package com.emall.operations;

import java.time.Instant;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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

@TableName("operations_approval")
record ApprovalRequest(@TableId(value = "approval_id", type = IdType.INPUT) long approvalId, String workflowType,
        String resourceType, String resourceId, String requester, String approver, String reason,
        ApprovalStatus status, Instant createdAt, Instant updatedAt) {
    ApprovalRequest decide(String operator, ApprovalStatus nextStatus) {
        return new ApprovalRequest(approvalId, workflowType, resourceType, resourceId, requester, operator, reason,
                nextStatus, createdAt, Instant.now());
    }
}

@TableName("operations_task")
record OperationTask(@TableId(value = "task_id", type = IdType.INPUT) long taskId, String taskType,
        String resourceType, String resourceId, String owner,
        TaskStatus status, int priority, String summary, Instant createdAt, Instant updatedAt) {
    OperationTask changeStatus(TaskStatus nextStatus) {
        return new OperationTask(taskId, taskType, resourceType, resourceId, owner, nextStatus, priority, summary,
                createdAt, Instant.now());
    }
}

@TableName("operations_compliance_evidence")
record ComplianceEvidence(@TableId(value = "evidence_id", type = IdType.INPUT) long evidenceId, String evidenceType,
        String resourceType, String resourceId, String owner, String summary, Instant createdAt) {
}

@TableName("operations_security_incident")
record SecurityIncident(@TableId(value = "incident_id", type = IdType.INPUT) long incidentId, String severity,
        String owner, String summary, IncidentStatus status, Instant createdAt, Instant updatedAt) {
    SecurityIncident changeStatus(IncidentStatus nextStatus) {
        return new SecurityIncident(incidentId, severity, owner, summary, nextStatus, createdAt, Instant.now());
    }
}

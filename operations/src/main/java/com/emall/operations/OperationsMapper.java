package com.emall.operations;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface OperationsMapper {
    @Insert("""
            INSERT INTO operations_approval
                (approval_id, workflow_type, resource_type, resource_id, requester, approver, reason, status,
                created_at, updated_at)
            VALUES (#{approval.approvalId}, #{approval.workflowType}, #{approval.resourceType},
                #{approval.resourceId}, #{approval.requester}, #{approval.approver}, #{approval.reason},
                #{approval.status}, #{approval.createdAt}, #{approval.updatedAt})
            ON DUPLICATE KEY UPDATE approver = VALUES(approver), status = VALUES(status),
                updated_at = VALUES(updated_at)
            """)
    int saveApproval(@Param("approval") ApprovalRequest approval);

    @Insert("""
            INSERT INTO operations_task
                (task_id, task_type, resource_type, resource_id, owner, status, priority, summary,
                created_at, updated_at)
            VALUES (#{task.taskId}, #{task.taskType}, #{task.resourceType}, #{task.resourceId}, #{task.owner},
                #{task.status}, #{task.priority}, #{task.summary}, #{task.createdAt}, #{task.updatedAt})
            ON DUPLICATE KEY UPDATE owner = VALUES(owner), status = VALUES(status), priority = VALUES(priority),
                summary = VALUES(summary), updated_at = VALUES(updated_at)
            """)
    int saveTask(@Param("task") OperationTask task);

    @Insert("""
            INSERT INTO operations_security_incident
                (incident_id, severity, owner, summary, status, created_at, updated_at)
            VALUES (#{incident.incidentId}, #{incident.severity}, #{incident.owner}, #{incident.summary},
                #{incident.status}, #{incident.createdAt}, #{incident.updatedAt})
            ON DUPLICATE KEY UPDATE owner = VALUES(owner), status = VALUES(status),
                updated_at = VALUES(updated_at)
            """)
    int saveIncident(@Param("incident") SecurityIncident incident);
}

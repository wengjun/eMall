package com.emall.operations;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    @Select("""
            SELECT approval_id, workflow_type, resource_type, resource_id, requester, approver, reason, status,
                created_at, updated_at
            FROM operations_approval
            WHERE approval_id = #{approvalId}
            """)
    ApprovalRequest findApproval(@Param("approvalId") long approvalId);

    @Select("""
            SELECT approval_id, workflow_type, resource_type, resource_id, requester, approver, reason, status,
                created_at, updated_at
            FROM operations_approval
            WHERE status = #{status}
            ORDER BY updated_at DESC
            """)
    List<ApprovalRequest> findApprovals(@Param("status") ApprovalStatus status);

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

    @Select("""
            SELECT task_id, task_type, resource_type, resource_id, owner, status, priority, summary, created_at,
                updated_at
            FROM operations_task
            WHERE task_id = #{taskId}
            """)
    OperationTask findTask(@Param("taskId") long taskId);

    @Select("""
            SELECT task_id, task_type, resource_type, resource_id, owner, status, priority, summary, created_at,
                updated_at
            FROM operations_task
            WHERE status = #{status}
            ORDER BY priority ASC, updated_at DESC
            """)
    List<OperationTask> findTasks(@Param("status") TaskStatus status);

    @Insert("""
            INSERT INTO operations_compliance_evidence
                (evidence_id, evidence_type, resource_type, resource_id, owner, summary, created_at)
            VALUES (#{evidence.evidenceId}, #{evidence.evidenceType}, #{evidence.resourceType},
                #{evidence.resourceId}, #{evidence.owner}, #{evidence.summary}, #{evidence.createdAt})
            """)
    int saveEvidence(@Param("evidence") ComplianceEvidence evidence);

    @Select("""
            SELECT evidence_id, evidence_type, resource_type, resource_id, owner, summary, created_at
            FROM operations_compliance_evidence
            WHERE resource_type = #{resourceType} AND resource_id = #{resourceId}
            ORDER BY created_at DESC
            """)
    List<ComplianceEvidence> findEvidence(@Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId);

    @Insert("""
            INSERT INTO operations_security_incident
                (incident_id, severity, owner, summary, status, created_at, updated_at)
            VALUES (#{incident.incidentId}, #{incident.severity}, #{incident.owner}, #{incident.summary},
                #{incident.status}, #{incident.createdAt}, #{incident.updatedAt})
            ON DUPLICATE KEY UPDATE owner = VALUES(owner), status = VALUES(status),
                updated_at = VALUES(updated_at)
            """)
    int saveIncident(@Param("incident") SecurityIncident incident);

    @Select("""
            SELECT incident_id, severity, owner, summary, status, created_at, updated_at
            FROM operations_security_incident
            WHERE incident_id = #{incidentId}
            """)
    SecurityIncident findIncident(@Param("incidentId") long incidentId);
}

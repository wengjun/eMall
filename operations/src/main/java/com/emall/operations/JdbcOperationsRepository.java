package com.emall.operations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcOperationsRepository implements OperationsRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcOperationsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ApprovalRequest saveApproval(ApprovalRequest approval) {
        jdbcTemplate.update("""
                INSERT INTO operations_approval
                    (approval_id, workflow_type, resource_type, resource_id, requester, approver, reason, status,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE approver = VALUES(approver), status = VALUES(status),
                    updated_at = VALUES(updated_at)
                """, approval.approvalId(), approval.workflowType(), approval.resourceType(), approval.resourceId(),
                approval.requester(), approval.approver(), approval.reason(), approval.status().name(),
                Timestamp.from(approval.createdAt()), Timestamp.from(approval.updatedAt()));
        return approval;
    }

    @Override
    public Optional<ApprovalRequest> findApproval(long approvalId) {
        return jdbcTemplate.query("SELECT * FROM operations_approval WHERE approval_id = ?",
                this::mapApproval, approvalId).stream().findFirst();
    }

    @Override
    public List<ApprovalRequest> findApprovals(ApprovalStatus status) {
        return jdbcTemplate.query("""
                SELECT * FROM operations_approval
                WHERE status = ?
                ORDER BY updated_at DESC
                """, this::mapApproval, status.name());
    }

    @Override
    public OperationTask saveTask(OperationTask task) {
        jdbcTemplate.update("""
                INSERT INTO operations_task
                    (task_id, task_type, resource_type, resource_id, owner, status, priority, summary,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE owner = VALUES(owner), status = VALUES(status), priority = VALUES(priority),
                    summary = VALUES(summary), updated_at = VALUES(updated_at)
                """, task.taskId(), task.taskType(), task.resourceType(), task.resourceId(), task.owner(),
                task.status().name(), task.priority(), task.summary(), Timestamp.from(task.createdAt()),
                Timestamp.from(task.updatedAt()));
        return task;
    }

    @Override
    public Optional<OperationTask> findTask(long taskId) {
        return jdbcTemplate.query("SELECT * FROM operations_task WHERE task_id = ?",
                this::mapTask, taskId).stream().findFirst();
    }

    @Override
    public List<OperationTask> findTasks(TaskStatus status) {
        return jdbcTemplate.query("""
                SELECT * FROM operations_task
                WHERE status = ?
                ORDER BY priority ASC, updated_at DESC
                """, this::mapTask, status.name());
    }

    @Override
    public ComplianceEvidence saveEvidence(ComplianceEvidence evidence) {
        jdbcTemplate.update("""
                INSERT INTO operations_compliance_evidence
                    (evidence_id, evidence_type, resource_type, resource_id, owner, summary, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, evidence.evidenceId(), evidence.evidenceType(), evidence.resourceType(), evidence.resourceId(),
                evidence.owner(), evidence.summary(), Timestamp.from(evidence.createdAt()));
        return evidence;
    }

    @Override
    public List<ComplianceEvidence> findEvidence(String resourceType, String resourceId) {
        return jdbcTemplate.query("""
                SELECT * FROM operations_compliance_evidence
                WHERE resource_type = ? AND resource_id = ?
                ORDER BY created_at DESC
                """, this::mapEvidence, resourceType, resourceId);
    }

    @Override
    public SecurityIncident saveIncident(SecurityIncident incident) {
        jdbcTemplate.update("""
                INSERT INTO operations_security_incident
                    (incident_id, severity, owner, summary, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE owner = VALUES(owner), status = VALUES(status),
                    updated_at = VALUES(updated_at)
                """, incident.incidentId(), incident.severity(), incident.owner(), incident.summary(),
                incident.status().name(), Timestamp.from(incident.createdAt()), Timestamp.from(incident.updatedAt()));
        return incident;
    }

    @Override
    public Optional<SecurityIncident> findIncident(long incidentId) {
        return jdbcTemplate.query("SELECT * FROM operations_security_incident WHERE incident_id = ?",
                this::mapIncident, incidentId).stream().findFirst();
    }

    private ApprovalRequest mapApproval(ResultSet rs, int rowNum) throws SQLException {
        return new ApprovalRequest(rs.getLong("approval_id"), rs.getString("workflow_type"),
                rs.getString("resource_type"), rs.getString("resource_id"), rs.getString("requester"),
                rs.getString("approver"), rs.getString("reason"), ApprovalStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private OperationTask mapTask(ResultSet rs, int rowNum) throws SQLException {
        return new OperationTask(rs.getLong("task_id"), rs.getString("task_type"), rs.getString("resource_type"),
                rs.getString("resource_id"), rs.getString("owner"), TaskStatus.valueOf(rs.getString("status")),
                rs.getInt("priority"), rs.getString("summary"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private ComplianceEvidence mapEvidence(ResultSet rs, int rowNum) throws SQLException {
        return new ComplianceEvidence(rs.getLong("evidence_id"), rs.getString("evidence_type"),
                rs.getString("resource_type"), rs.getString("resource_id"), rs.getString("owner"),
                rs.getString("summary"), rs.getTimestamp("created_at").toInstant());
    }

    private SecurityIncident mapIncident(ResultSet rs, int rowNum) throws SQLException {
        return new SecurityIncident(rs.getLong("incident_id"), rs.getString("severity"), rs.getString("owner"),
                rs.getString("summary"), IncidentStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}

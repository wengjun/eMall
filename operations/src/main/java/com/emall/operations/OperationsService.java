package com.emall.operations;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OperationsService {
    private final OperationsRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    OperationsService(OperationsRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    ApprovalRequest createApproval(String workflowType, String resourceType, String resourceId, String requester,
                                   String reason) {
        Instant now = Instant.now();
        return repository.saveApproval(new ApprovalRequest(idGenerator.nextId(), normalize(workflowType),
                normalize(resourceType), normalize(resourceId), normalize(requester), "", reason,
                ApprovalStatus.PENDING, now, now));
    }

    @Transactional
    ApprovalRequest decideApproval(long approvalId, String operator, ApprovalStatus status) {
        if (status != ApprovalStatus.APPROVED && status != ApprovalStatus.REJECTED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "approval decision must be approved or rejected");
        }
        ApprovalRequest approval = repository.findApproval(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "approval request not found"));
        if (approval.status() != ApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "approval request is already decided");
        }
        return repository.saveApproval(approval.decide(normalize(operator), status));
    }

    List<ApprovalRequest> findApprovals(ApprovalStatus status) {
        return repository.findApprovals(status);
    }

    @Transactional
    OperationTask createTask(String taskType, String resourceType, String resourceId, String owner, int priority,
                             String summary) {
        Instant now = Instant.now();
        return repository.saveTask(new OperationTask(idGenerator.nextId(), normalize(taskType),
                normalize(resourceType), normalize(resourceId), normalize(owner), TaskStatus.OPEN,
                Math.max(1, Math.min(priority, 5)), summary, now, now));
    }

    @Transactional
    OperationTask changeTaskStatus(long taskId, TaskStatus status) {
        OperationTask task = repository.findTask(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "operation task not found"));
        return repository.saveTask(task.changeStatus(status));
    }

    List<OperationTask> findTasks(TaskStatus status) {
        return repository.findTasks(status);
    }

    @Transactional
    ComplianceEvidence recordEvidence(String evidenceType, String resourceType, String resourceId, String owner,
                                      String summary) {
        return repository.saveEvidence(new ComplianceEvidence(idGenerator.nextId(), normalize(evidenceType),
                normalize(resourceType), normalize(resourceId), normalize(owner), summary, Instant.now()));
    }

    List<ComplianceEvidence> findEvidence(String resourceType, String resourceId) {
        return repository.findEvidence(normalize(resourceType), normalize(resourceId));
    }

    @Transactional
    SecurityIncident openIncident(String severity, String owner, String summary) {
        Instant now = Instant.now();
        return repository.saveIncident(new SecurityIncident(idGenerator.nextId(), normalize(severity),
                normalize(owner), summary, IncidentStatus.OPEN, now, now));
    }

    @Transactional
    SecurityIncident changeIncidentStatus(long incidentId, IncidentStatus status) {
        SecurityIncident incident = repository.findIncident(incidentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "security incident not found"));
        return repository.saveIncident(incident.changeStatus(status));
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "operations value must not be blank");
        }
        return normalized;
    }
}

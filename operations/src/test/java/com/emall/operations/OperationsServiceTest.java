package com.emall.operations;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;

class OperationsServiceTest {
    private final InMemoryOperationsRepository repository = new InMemoryOperationsRepository();
    private final OperationsService service = new OperationsService(repository, new SnowflakeIdGenerator(23L));

    @Test
    void approvalCanBeCreatedAndApproved() {
        ApprovalRequest approval = service.createApproval("refund", "order", "1001", "ops-a", "large refund");

        ApprovalRequest approved = service.decideApproval(approval.approvalId(), "ops-lead", ApprovalStatus.APPROVED);

        assertThat(approved.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(service.findApprovals(ApprovalStatus.PENDING)).isEmpty();
    }

    @Test
    void recordsComplianceEvidenceAndSecurityIncident() {
        service.recordEvidence("pii-access-review", "user", "1001", "security", "reviewed export request");
        SecurityIncident incident = service.openIncident("high", "secops", "credential leakage");

        SecurityIncident closed = service.changeIncidentStatus(incident.incidentId(), IncidentStatus.CLOSED);

        assertThat(service.findEvidence("user", "1001")).hasSize(1);
        assertThat(closed.status()).isEqualTo(IncidentStatus.CLOSED);
    }

    @Test
    void taskStatusCanMoveToResolved() {
        OperationTask task =
                service.createTask("reconciliation", "payment", "p1001", "finance", 1, "review channel mismatch");

        OperationTask resolved = service.changeTaskStatus(task.taskId(), TaskStatus.RESOLVED);

        assertThat(resolved.status()).isEqualTo(TaskStatus.RESOLVED);
    }
}

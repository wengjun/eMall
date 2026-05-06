CREATE TABLE operations_approval (
    approval_id BIGINT NOT NULL,
    workflow_type VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    requester VARCHAR(128) NOT NULL,
    approver VARCHAR(128) NOT NULL,
    reason VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (approval_id),
    KEY idx_operations_approval_status (status, updated_at),
    KEY idx_operations_approval_resource (resource_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE operations_task (
    task_id BIGINT NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    owner VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority INT NOT NULL,
    summary VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (task_id),
    KEY idx_operations_task_status_priority (status, priority, updated_at),
    KEY idx_operations_task_owner_status (owner, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE operations_compliance_evidence (
    evidence_id BIGINT NOT NULL,
    evidence_type VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    owner VARCHAR(128) NOT NULL,
    summary VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (evidence_id),
    KEY idx_operations_evidence_resource (resource_type, resource_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE operations_security_incident (
    incident_id BIGINT NOT NULL,
    severity VARCHAR(32) NOT NULL,
    owner VARCHAR(128) NOT NULL,
    summary VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (incident_id),
    KEY idx_operations_incident_status (status, severity, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

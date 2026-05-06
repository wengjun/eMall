CREATE TABLE capacity_rehearsal (
    rehearsal_id BIGINT PRIMARY KEY,
    service_name VARCHAR(128) NOT NULL,
    target_qps INT NOT NULL,
    peak_concurrency INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_capacity_rehearsal_service (service_name)
);

CREATE TABLE slo_objective (
    slo_id BIGINT PRIMARY KEY,
    service_name VARCHAR(128) NOT NULL,
    availability_target DECIMAL(10, 6) NOT NULL,
    latency_p95_ms INT NOT NULL,
    error_budget_percent DECIMAL(10, 6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE chaos_schedule (
    chaos_id BIGINT PRIMARY KEY,
    service_name VARCHAR(128) NOT NULL,
    drill_type VARCHAR(128) NOT NULL,
    blast_radius_percent INT NOT NULL,
    approval_status VARCHAR(32) NOT NULL,
    scheduled_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE readiness_gate (
    gate_id BIGINT PRIMARY KEY,
    service_name VARCHAR(128) NOT NULL,
    runbook_ready BOOLEAN NOT NULL,
    dashboard_ready BOOLEAN NOT NULL,
    rollback_ready BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_readiness_gate_status (status)
);

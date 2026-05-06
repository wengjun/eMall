CREATE TABLE backup_plan (
    plan_id BIGINT PRIMARY KEY,
    database_name VARCHAR(128) NOT NULL,
    backup_type VARCHAR(64) NOT NULL,
    retention_days INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_backup_plan_database (database_name)
);

CREATE TABLE database_operation (
    operation_id BIGINT PRIMARY KEY,
    database_name VARCHAR(128) NOT NULL,
    operation_type VARCHAR(128) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    detail VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_database_operation_status (status)
);

CREATE TABLE finops_action (
    action_id BIGINT PRIMARY KEY,
    service_name VARCHAR(128) NOT NULL,
    action_type VARCHAR(128) NOT NULL,
    estimated_saving DECIMAL(18, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_finops_action_status (status)
);

CREATE TABLE security_operation (
    operation_id BIGINT PRIMARY KEY,
    service_name VARCHAR(128) NOT NULL,
    signal_type VARCHAR(128) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_security_operation_risk (risk_level, status)
);

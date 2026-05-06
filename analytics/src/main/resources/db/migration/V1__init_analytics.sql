CREATE TABLE metric_definition (
    metric_id BIGINT PRIMARY KEY,
    metric_name VARCHAR(128) NOT NULL,
    owner VARCHAR(128) NOT NULL,
    expression TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_metric_definition_name (metric_name)
);

CREATE TABLE metric_point (
    point_id BIGINT PRIMARY KEY,
    metric_name VARCHAR(128) NOT NULL,
    dimension_key VARCHAR(128) NOT NULL,
    metric_value DECIMAL(19, 6) NOT NULL,
    event_time TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_metric_point_metric_time (metric_name, event_time)
);

CREATE TABLE dashboard_definition (
    dashboard_id BIGINT PRIMARY KEY,
    dashboard_name VARCHAR(128) NOT NULL,
    business_domain VARCHAR(128) NOT NULL,
    metric_names TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE anomaly_signal (
    anomaly_id BIGINT PRIMARY KEY,
    metric_name VARCHAR(128) NOT NULL,
    actual_value DECIMAL(19, 6) NOT NULL,
    expected_value DECIMAL(19, 6) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_anomaly_signal_metric (metric_name, created_at)
);

CREATE TABLE consent_record (
    consent_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    purpose VARCHAR(128) NOT NULL,
    granted BOOLEAN NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_consent_record_user_purpose (user_id, purpose)
);

CREATE TABLE privacy_request (
    request_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    request_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_privacy_request_user (user_id),
    KEY idx_privacy_request_status (status)
);

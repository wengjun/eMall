CREATE TABLE traffic_control_rule (
    rule_id BIGINT PRIMARY KEY,
    resource VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    dimension VARCHAR(64) NOT NULL,
    match_value VARCHAR(128) NOT NULL,
    threshold_value INT NOT NULL,
    unit_code VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_traffic_control_rule (resource, type, dimension, match_value, unit_code),
    KEY idx_traffic_control_rule_enabled (enabled, resource, type)
);

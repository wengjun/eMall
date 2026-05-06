CREATE TABLE risk_rule (
    rule_id BIGINT NOT NULL,
    scene VARCHAR(64) NOT NULL,
    rule_code VARCHAR(128) NOT NULL,
    field_name VARCHAR(64) NOT NULL,
    operator VARCHAR(32) NOT NULL,
    threshold_value DECIMAL(18, 6) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (rule_id),
    KEY idx_risk_rule_scene_status (scene, status, updated_at),
    UNIQUE KEY uk_risk_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE risk_device_reputation (
    device_id VARCHAR(128) NOT NULL,
    reputation_score INT NOT NULL,
    risky BOOLEAN NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (device_id),
    KEY idx_risk_device_risky (risky, reputation_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE risk_event (
    event_id BIGINT NOT NULL,
    scene VARCHAR(64) NOT NULL,
    subject_id VARCHAR(128) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    ip VARCHAR(64) NOT NULL,
    amount DECIMAL(18, 6) NOT NULL,
    velocity INT NOT NULL,
    score INT NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    reason VARCHAR(256) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_risk_event_subject_time (subject_id, occurred_at),
    KEY idx_risk_event_scene_level_time (scene, risk_level, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

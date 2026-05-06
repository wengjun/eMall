CREATE TABLE feature_toggle (
    toggle_id BIGINT PRIMARY KEY,
    flag_key VARCHAR(128) NOT NULL,
    service_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    rollout_percent INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_feature_toggle_flag (flag_key),
    KEY idx_feature_toggle_service (service_name)
);

CREATE TABLE rollout_plan (
    rollout_id BIGINT PRIMARY KEY,
    service_name VARCHAR(128) NOT NULL,
    version VARCHAR(64) NOT NULL,
    strategy VARCHAR(64) NOT NULL,
    current_percent INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_rollout_plan_status (status)
);

CREATE TABLE message_topic_governance (
    topic_id BIGINT PRIMARY KEY,
    topic_name VARCHAR(128) NOT NULL,
    owner VARCHAR(128) NOT NULL,
    schema_version VARCHAR(64) NOT NULL,
    lag_budget BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_message_topic_governance_name (topic_name)
);

CREATE TABLE replay_plan (
    replay_id BIGINT PRIMARY KEY,
    topic_name VARCHAR(128) NOT NULL,
    consumer_group VARCHAR(128) NOT NULL,
    from_offset BIGINT NOT NULL,
    to_offset BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_replay_plan_topic_status (topic_name, status)
);

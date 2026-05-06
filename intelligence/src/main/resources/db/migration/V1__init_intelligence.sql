CREATE TABLE user_profile (
    profile_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    segment VARCHAR(128) NOT NULL,
    preferences TEXT NOT NULL,
    privacy_restricted BOOLEAN NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_user_profile_user (user_id)
);

CREATE TABLE item_profile (
    profile_id BIGINT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    category VARCHAR(128) NOT NULL,
    attributes TEXT NOT NULL,
    quality_score DECIMAL(10, 4) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_item_profile_sku (sku_id)
);

CREATE TABLE feature_definition (
    feature_id BIGINT PRIMARY KEY,
    feature_name VARCHAR(128) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    owner VARCHAR(128) NOT NULL,
    freshness_seconds INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_feature_definition_name (feature_name)
);

CREATE TABLE online_feature_value (
    value_id BIGINT PRIMARY KEY,
    feature_name VARCHAR(128) NOT NULL,
    entity_key VARCHAR(128) NOT NULL,
    feature_value VARCHAR(512) NOT NULL,
    event_time TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_online_feature_value_lookup (feature_name, entity_key)
);

CREATE TABLE model_deployment (
    model_id BIGINT PRIMARY KEY,
    model_name VARCHAR(128) NOT NULL,
    version VARCHAR(64) NOT NULL,
    use_case VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    approval_ticket VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_model_deployment_use_case (use_case, status)
);

CREATE TABLE ai_decision (
    decision_id BIGINT PRIMARY KEY,
    use_case VARCHAR(128) NOT NULL,
    entity_key VARCHAR(128) NOT NULL,
    decision VARCHAR(128) NOT NULL,
    score DECIMAL(10, 6) NOT NULL,
    model_version VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_ai_decision_use_case (use_case, created_at)
);

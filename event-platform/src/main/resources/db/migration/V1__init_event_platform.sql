CREATE TABLE event_schema (
    schema_id BIGINT PRIMARY KEY,
    event_name VARCHAR(128) NOT NULL,
    version INT NOT NULL,
    owner VARCHAR(128) NOT NULL,
    json_schema TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_event_schema_name_version (event_name, version)
);

CREATE TABLE tracking_event (
    event_id BIGINT PRIMARY KEY,
    event_name VARCHAR(128) NOT NULL,
    version INT NOT NULL,
    event_key VARCHAR(128) NOT NULL,
    user_key VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    late_event BOOLEAN NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    ingested_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_tracking_event_key (event_key),
    KEY idx_tracking_event_name (event_name)
);

CREATE TABLE pipeline_offset (
    offset_id BIGINT PRIMARY KEY,
    consumer_group VARCHAR(128) NOT NULL,
    topic_name VARCHAR(128) NOT NULL,
    processed_offset BIGINT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_pipeline_offset_group_topic (consumer_group, topic_name)
);

CREATE TABLE metric_materialization (
    materialization_id BIGINT PRIMARY KEY,
    metric_name VARCHAR(128) NOT NULL,
    window_key VARCHAR(128) NOT NULL,
    event_count BIGINT NOT NULL,
    late_event_count BIGINT NOT NULL,
    materialized_at TIMESTAMP(6) NOT NULL,
    KEY idx_metric_materialization_metric (metric_name, window_key)
);

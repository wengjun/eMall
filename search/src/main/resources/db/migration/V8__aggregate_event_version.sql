CREATE TABLE consumed_aggregate_version (
    consumer_aggregate_id VARCHAR(320) NOT NULL,
    consumer_name VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (consumer_aggregate_id),
    UNIQUE KEY uk_consumed_aggregate_coordinate (consumer_name, aggregate_type, aggregate_id),
    KEY idx_consumed_aggregate_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE identity_lifecycle (
    account_id BIGINT NOT NULL,
    registration_id VARCHAR(128) NOT NULL,
    request_fingerprint CHAR(64) NOT NULL,
    binding_hash CHAR(64) NOT NULL,
    projection_status VARCHAR(32) NOT NULL,
    last_published_version BIGINT NOT NULL DEFAULT 0,
    last_acknowledged_version BIGINT NOT NULL DEFAULT 0,
    reconciliation_partition INT NOT NULL DEFAULT 0,
    next_reconcile_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (account_id),
    UNIQUE KEY uk_identity_lifecycle_registration (registration_id),
    KEY idx_identity_lifecycle_reconcile (reconciliation_partition, next_reconcile_at, account_id),
    CONSTRAINT fk_identity_lifecycle_account FOREIGN KEY (account_id)
        REFERENCES identity_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_event (
    event_id VARCHAR(128) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    schema_version INT NOT NULL,
    aggregate_version BIGINT NOT NULL,
    producer VARCHAR(64) NOT NULL,
    producer_version VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    trace_id VARCHAR(64) NULL,
    correlation_id VARCHAR(128) NULL,
    shard_id INT NOT NULL DEFAULT 0,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP(6) NOT NULL,
    claimed_by VARCHAR(128) NULL,
    claimed_until TIMESTAMP(6) NULL,
    published_at TIMESTAMP(6) NULL,
    error_code VARCHAR(64) NULL,
    last_error VARCHAR(512) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_identity_outbox_claim (status, shard_id, next_retry_at, claimed_until, created_at),
    KEY idx_identity_outbox_aggregate (aggregate_type, aggregate_id, aggregate_version, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_aggregate_sequence (
    aggregate_key VARCHAR(256) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (aggregate_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE processed_message (
    message_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSED',
    retry_count INT NOT NULL DEFAULT 0,
    last_error_code VARCHAR(64) NULL,
    last_error VARCHAR(1024) NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    dead_at TIMESTAMP(6) NULL,
    PRIMARY KEY (message_id),
    KEY idx_identity_processed_time (processed_at),
    KEY idx_identity_processed_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE consumed_aggregate_version (
    consumer_aggregate_id VARCHAR(320) NOT NULL,
    consumer_name VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (consumer_aggregate_id),
    UNIQUE KEY uk_identity_consumed_coordinate (consumer_name, aggregate_type, aggregate_id),
    KEY idx_identity_consumed_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE scheduled_task_lock (
    lock_name VARCHAR(128) NOT NULL,
    owner_id VARCHAR(256) NOT NULL,
    locked_until TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (lock_name),
    KEY idx_identity_task_lock_until (locked_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

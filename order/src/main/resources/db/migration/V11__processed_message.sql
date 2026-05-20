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
    KEY idx_processed_message_time (processed_at),
    KEY idx_processed_message_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

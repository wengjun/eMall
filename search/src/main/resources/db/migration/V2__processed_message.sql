CREATE TABLE processed_message (
    message_id VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (message_id),
    KEY idx_processed_message_time (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE processed_message
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PROCESSED' AFTER message_id,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN last_error_code VARCHAR(64) NULL AFTER retry_count,
    ADD COLUMN last_error VARCHAR(1024) NULL AFTER last_error_code,
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER processed_at,
    ADD COLUMN dead_at TIMESTAMP(6) NULL AFTER updated_at,
    ADD KEY idx_processed_message_status (status, updated_at),
    ADD KEY idx_processed_message_processing_reclaim (status, updated_at);

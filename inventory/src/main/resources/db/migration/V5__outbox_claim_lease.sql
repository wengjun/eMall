ALTER TABLE outbox_event
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0 AFTER event_type,
    ADD COLUMN claimed_by VARCHAR(128) AFTER next_retry_at,
    ADD COLUMN claimed_until TIMESTAMP(6) AFTER claimed_by,
    ADD COLUMN published_at TIMESTAMP(6) AFTER claimed_until,
    ADD COLUMN error_code VARCHAR(64) AFTER published_at,
    ADD COLUMN last_error VARCHAR(512) AFTER error_code,
    ADD KEY idx_outbox_claim (status, shard_id, next_retry_at, claimed_until, created_at);

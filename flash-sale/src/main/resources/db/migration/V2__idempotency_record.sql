CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(512) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id VARCHAR(128) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    request_digest CHAR(64),
    response_digest CHAR(64),
    status VARCHAR(32) NOT NULL,
    locked_until TIMESTAMP(6),
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (idempotency_key),
    KEY idx_idempotency_status_lock (status, locked_until),
    KEY idx_idempotency_expire (expires_at),
    KEY idx_idempotency_owner_request (owner_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

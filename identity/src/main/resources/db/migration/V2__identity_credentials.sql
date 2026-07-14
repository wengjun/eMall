CREATE TABLE identity_credential (
    account_id BIGINT NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP(6) NULL,
    password_changed_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE identity_device_session
    ADD UNIQUE KEY uk_identity_session_refresh_token (refresh_token);

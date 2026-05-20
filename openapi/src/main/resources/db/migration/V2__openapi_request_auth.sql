ALTER TABLE openapi_app
    ADD COLUMN secret_ciphertext VARCHAR(1024) NULL AFTER secret_hash;

CREATE TABLE openapi_nonce (
    app_key VARCHAR(128) NOT NULL,
    nonce VARCHAR(128) NOT NULL,
    request_path VARCHAR(256) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (app_key, nonce),
    KEY idx_openapi_nonce_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

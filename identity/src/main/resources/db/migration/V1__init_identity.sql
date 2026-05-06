CREATE TABLE identity_account (
    account_id BIGINT NOT NULL,
    identity_type VARCHAR(32) NOT NULL,
    subject VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (account_id),
    UNIQUE KEY uk_identity_account_subject (subject)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE identity_device_session (
    session_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    access_token VARCHAR(128) NOT NULL,
    refresh_token VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (session_id),
    KEY idx_identity_session_account_status (account_id, status),
    UNIQUE KEY uk_identity_session_access_token (access_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE identity_permission_grant (
    grant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    scope VARCHAR(128) NOT NULL,
    resource VARCHAR(256) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (grant_id),
    KEY idx_identity_grant_account_scope (account_id, scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE identity_service_client (
    client_id BIGINT NOT NULL,
    client_key VARCHAR(128) NOT NULL,
    secret_hash VARCHAR(128) NOT NULL,
    scopes VARCHAR(512) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (client_id),
    UNIQUE KEY uk_identity_client_key (client_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE identity_merchant_sub_account (
    sub_account_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (sub_account_id),
    KEY idx_identity_sub_account_merchant (merchant_id, active),
    UNIQUE KEY uk_identity_sub_account (merchant_id, account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

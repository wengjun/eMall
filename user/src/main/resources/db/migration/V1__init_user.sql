CREATE TABLE user_account (
    user_id BIGINT NOT NULL,
    mobile VARCHAR(32) NOT NULL,
    nickname VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_mobile (mobile),
    KEY idx_user_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE openapi_app (
    app_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    app_key VARCHAR(128) NOT NULL,
    secret_hash VARCHAR(128) NOT NULL,
    name VARCHAR(128) NOT NULL,
    scopes VARCHAR(512) NOT NULL,
    daily_quota INT NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (app_id),
    UNIQUE KEY uk_openapi_app_key (app_key),
    KEY idx_openapi_app_merchant (merchant_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE openapi_quota_usage (
    app_key VARCHAR(128) NOT NULL,
    usage_date DATE NOT NULL,
    used_count INT NOT NULL,
    daily_quota INT NOT NULL,
    allowed BOOLEAN NOT NULL,
    PRIMARY KEY (app_key, usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE openapi_webhook_subscription (
    subscription_id BIGINT NOT NULL,
    app_id BIGINT NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    target_url VARCHAR(512) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (subscription_id),
    KEY idx_openapi_webhook_app_event (app_id, event_type, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE openapi_webhook_delivery (
    delivery_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (delivery_id),
    KEY idx_openapi_delivery_subscription_status (subscription_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

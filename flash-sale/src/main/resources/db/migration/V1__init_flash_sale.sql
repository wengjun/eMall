CREATE TABLE flash_sale_campaign (
    campaign_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    per_user_limit INT NOT NULL,
    token_ttl_seconds INT NOT NULL,
    queue_capacity INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (campaign_id),
    KEY idx_flash_sale_campaign_sku_time (sku_id, starts_at, ends_at),
    KEY idx_flash_sale_campaign_status_time (status, starts_at, ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE flash_sale_stock (
    campaign_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    total_stock INT NOT NULL,
    available_stock INT NOT NULL,
    token_reserved_stock INT NOT NULL,
    queued_stock INT NOT NULL,
    sold_stock INT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (campaign_id),
    KEY idx_flash_sale_stock_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE flash_sale_token (
    token_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    token VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (token_id),
    UNIQUE KEY uk_flash_sale_token_value (token),
    KEY idx_flash_sale_token_user_campaign (campaign_id, user_id),
    KEY idx_flash_sale_token_expiry (expires_at, used)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE flash_sale_order_request (
    request_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    token VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (request_id),
    UNIQUE KEY uk_flash_sale_request_token (token),
    KEY idx_flash_sale_request_queue (campaign_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fulfillment_order (
    fulfillment_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    warehouse_code VARCHAR(64) NOT NULL,
    carrier VARCHAR(64),
    tracking_no VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (fulfillment_id),
    UNIQUE KEY uk_fulfillment_order (order_id),
    KEY idx_fulfillment_status_updated (status, updated_at),
    KEY idx_fulfillment_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE processed_message (
    message_id VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (message_id),
    KEY idx_processed_message_time (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

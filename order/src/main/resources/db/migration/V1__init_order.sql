CREATE TABLE order_record (
    order_id BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    subtotal_amount DECIMAL(19, 2) NOT NULL,
    discount_amount DECIMAL(19, 2) NOT NULL,
    payable_amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    price_version BIGINT NOT NULL,
    coupon_id VARCHAR(128),
    inventory_reservation_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(512),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_order_request (request_id),
    KEY idx_order_user_created (user_id, created_at),
    KEY idx_order_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_event (
    event_id VARCHAR(128) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL,
    next_retry_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_outbox_publishable (status, next_retry_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE internal_operation_audit (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    service_name VARCHAR(64) NOT NULL,
    operation VARCHAR(128) NOT NULL,
    operator VARCHAR(128) NOT NULL,
    trace_id VARCHAR(128),
    affected INT NOT NULL,
    success BOOLEAN NOT NULL,
    message VARCHAR(512) NOT NULL,
    executed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_internal_operation_time (operation, executed_at),
    KEY idx_internal_operator_time (operator, executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

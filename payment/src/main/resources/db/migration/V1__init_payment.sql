CREATE TABLE payment_order (
    payment_id BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    channel_trade_no VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    order_confirmed BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_payment_request (request_id),
    UNIQUE KEY uk_payment_trade_no (channel_trade_no),
    KEY idx_payment_order (order_id),
    KEY idx_payment_status_confirmed_updated (status, order_confirmed, updated_at)
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

CREATE TABLE payment_ledger_entry (
    ledger_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    direction VARCHAR(16) NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    reference_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (ledger_id),
    UNIQUE KEY uk_payment_ledger_reference (reference_id),
    KEY idx_payment_ledger_payment (payment_id),
    KEY idx_payment_ledger_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment_channel_statement (
    statement_id BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    channel_trade_no VARCHAR(128) NOT NULL,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    statement_type VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    reconciled BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (statement_id),
    UNIQUE KEY uk_channel_statement_trade_type (channel_trade_no, statement_type),
    KEY idx_channel_statement_reconcile (reconciled, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment_reconciliation_record (
    record_id BIGINT NOT NULL,
    statement_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    channel_trade_no VARCHAR(128) NOT NULL,
    statement_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    message VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_reconciliation_statement (statement_id),
    KEY idx_reconciliation_status_time (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

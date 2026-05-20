ALTER TABLE payment_order
    DROP INDEX uk_payment_trade_no,
    ADD UNIQUE KEY uk_payment_channel_trade_no (channel, channel_trade_no);

ALTER TABLE payment_ledger_entry
    ADD COLUMN account_code VARCHAR(64) NOT NULL DEFAULT 'LEGACY' AFTER direction,
    DROP INDEX uk_payment_ledger_reference,
    ADD UNIQUE KEY uk_payment_ledger_reference_leg (reference_id, account_code, direction);

CREATE TABLE payment_refund_order (
    refund_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    channel_refund_no VARCHAR(128),
    amount DECIMAL(18, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(256) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (refund_id),
    UNIQUE KEY uk_refund_request (request_id),
    UNIQUE KEY uk_refund_channel_no (channel, channel_refund_no),
    KEY idx_refund_payment (payment_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

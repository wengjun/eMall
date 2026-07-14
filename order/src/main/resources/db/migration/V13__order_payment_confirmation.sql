CREATE TABLE order_payment_confirmation (
    order_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    paid_amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    channel_trade_no VARCHAR(128) NOT NULL,
    confirmed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_order_payment_confirmation_payment (payment_id),
    UNIQUE KEY uk_order_payment_confirmation_trade (channel_trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

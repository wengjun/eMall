CREATE TABLE payment_route_index (
    payment_id BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_payment_route_request (request_id),
    KEY idx_payment_route_order (order_id),
    KEY idx_payment_route_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_route_index (
    order_id BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_order_route_request (request_id),
    KEY idx_order_route_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

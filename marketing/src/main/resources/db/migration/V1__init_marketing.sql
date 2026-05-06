CREATE TABLE coupon (
    coupon_id VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    threshold_amount DECIMAL(18, 2) NOT NULL,
    discount_amount DECIMAL(18, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (coupon_id),
    KEY idx_coupon_user_status_expire (user_id, status, expires_at),
    KEY idx_coupon_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

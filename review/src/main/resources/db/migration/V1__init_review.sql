CREATE TABLE product_review (
    review_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (review_id),
    UNIQUE KEY uk_review_order_sku_user (order_id, sku_id, user_id),
    KEY idx_review_sku_status_created (sku_id, status, created_at),
    KEY idx_review_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

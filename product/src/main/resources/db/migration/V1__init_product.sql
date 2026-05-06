CREATE TABLE product (
    sku_id BIGINT NOT NULL,
    spu_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    category VARCHAR(128) NOT NULL,
    price DECIMAL(18, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (sku_id),
    KEY idx_product_spu (spu_id),
    KEY idx_product_status_updated (status, updated_at),
    KEY idx_product_category_status (category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO product (sku_id, spu_id, title, category, price, status, created_at, updated_at)
VALUES
    (10001, 90001, 'flagship phone', 'digital', 3999.00, 'ON_SALE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    (10002, 90002, 'thin laptop', 'computer', 6999.00, 'ON_SALE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

CREATE TABLE price_book (
    sku_id BIGINT NOT NULL,
    list_price DECIMAL(18, 2) NOT NULL,
    sale_price DECIMAL(18, 2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    version BIGINT NOT NULL,
    active BOOLEAN NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (sku_id),
    KEY idx_price_active_updated (active, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO price_book (sku_id, list_price, sale_price, currency, version, active, updated_at)
VALUES
    (10001, 3999.00, 3799.00, 'USD', 1, true, UTC_TIMESTAMP(6)),
    (10002, 6999.00, 6799.00, 'USD', 1, true, UTC_TIMESTAMP(6));

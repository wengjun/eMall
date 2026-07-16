ALTER TABLE inventory_item
    ADD COLUMN inventory_mode VARCHAR(32) NOT NULL DEFAULT 'SINGLE_ROW' AFTER sold,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER inventory_mode,
    ADD KEY idx_inventory_item_mode (inventory_mode, sku_id);

CREATE TABLE inventory_stock_ledger (
    ledger_id VARCHAR(192) NOT NULL,
    sku_id BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    operation VARCHAR(32) NOT NULL,
    bucket_no INT,
    total_delta BIGINT NOT NULL,
    reserved_delta BIGINT NOT NULL,
    sold_delta BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (ledger_id),
    KEY idx_inventory_ledger_sku_time (sku_id, created_at, ledger_id),
    KEY idx_inventory_ledger_request (request_id, operation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE inventory_bucket bucket
JOIN inventory_item item ON item.sku_id = bucket.sku_id
JOIN (
    SELECT sku_id, MIN(bucket_no) AS first_bucket, SUM(total) AS bucket_total
    FROM inventory_bucket
    GROUP BY sku_id
) summary ON summary.sku_id = bucket.sku_id AND summary.first_bucket = bucket.bucket_no
SET bucket.total = bucket.total + GREATEST(item.total - item.reserved - item.sold - summary.bucket_total, 0),
    bucket.updated_at = UTC_TIMESTAMP(6);

UPDATE inventory_item item
SET item.total = item.reserved + item.sold,
    item.inventory_mode = 'BUCKETED',
    item.version = item.version + 1,
    item.updated_at = UTC_TIMESTAMP(6)
WHERE EXISTS (
    SELECT 1
    FROM inventory_bucket bucket
    WHERE bucket.sku_id = item.sku_id
);

INSERT INTO inventory_stock_ledger (
    ledger_id, sku_id, request_id, operation, bucket_no,
    total_delta, reserved_delta, sold_delta, created_at
)
SELECT CONCAT('migration-v10:', item.sku_id),
       item.sku_id,
       'migration-v10',
       'MIGRATION_BASELINE',
       NULL,
       item.total + COALESCE(summary.total, 0),
       item.reserved + COALESCE(summary.reserved, 0),
       item.sold + COALESCE(summary.sold, 0),
       UTC_TIMESTAMP(6)
FROM inventory_item item
LEFT JOIN (
    SELECT sku_id, SUM(total) AS total, SUM(reserved) AS reserved, SUM(sold) AS sold
    FROM inventory_bucket
    GROUP BY sku_id
) summary ON summary.sku_id = item.sku_id;

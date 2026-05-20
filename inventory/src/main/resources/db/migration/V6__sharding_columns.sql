ALTER TABLE inventory_item
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_inventory_item_shard (shard_id, sku_id),
    ADD KEY idx_inventory_item_cell (cell_id, sku_id);

ALTER TABLE inventory_bucket
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_inventory_bucket_shard (shard_id, sku_id, bucket_no);

ALTER TABLE inventory_reservation
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_inventory_reservation_shard_status (shard_id, status, expires_at);

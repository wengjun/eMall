ALTER TABLE price_book
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_price_book_shard_sku (shard_id, sku_id),
    ADD KEY idx_price_book_cell_active (cell_id, active);

ALTER TABLE cart_item
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_cart_item_shard_user (shard_id, user_id),
    ADD KEY idx_cart_item_cell_user (cell_id, user_id);

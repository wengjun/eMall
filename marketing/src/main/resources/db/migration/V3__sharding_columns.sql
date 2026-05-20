ALTER TABLE coupon
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_coupon_shard_user (shard_id, user_id),
    ADD KEY idx_coupon_cell_status (cell_id, status);

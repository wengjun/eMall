ALTER TABLE order_record
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_order_shard_biz_date (shard_id, biz_date),
    ADD KEY idx_order_cell_status (cell_id, status);

ALTER TABLE outbox_event
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_order_outbox_shard_biz_date (shard_id, biz_date);

ALTER TABLE payment_order
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_payment_order_shard_status (shard_id, status),
    ADD KEY idx_payment_order_cell_created (cell_id, created_at);

ALTER TABLE payment_ledger_entry
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_payment_ledger_shard_payment (shard_id, payment_id);

ALTER TABLE payment_channel_statement
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_payment_statement_shard_reconciled (shard_id, reconciled);

ALTER TABLE payment_reconciliation_record
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_payment_reconciliation_shard_status (shard_id, status);

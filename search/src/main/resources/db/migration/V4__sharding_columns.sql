ALTER TABLE search_document
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_search_document_shard_sku (shard_id, sku_id),
    ADD KEY idx_search_document_cell_saleable (cell_id, saleable);

ALTER TABLE processed_message
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_processed_message_shard_processed (shard_id, processed_at);

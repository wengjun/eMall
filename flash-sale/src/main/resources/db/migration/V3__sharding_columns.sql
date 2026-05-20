ALTER TABLE flash_sale_campaign
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_flash_sale_campaign_shard_status (shard_id, status);

ALTER TABLE flash_sale_stock
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_flash_sale_stock_shard_campaign (shard_id, campaign_id);

ALTER TABLE flash_sale_token
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_flash_sale_token_shard_user (shard_id, user_id, used);

ALTER TABLE flash_sale_order_request
    ADD COLUMN shard_id INT NOT NULL DEFAULT 0,
    ADD COLUMN cell_id VARCHAR(64) NOT NULL DEFAULT 'cell-a',
    ADD COLUMN biz_date DATE NULL,
    ADD KEY idx_flash_sale_order_request_shard_status (shard_id, status);

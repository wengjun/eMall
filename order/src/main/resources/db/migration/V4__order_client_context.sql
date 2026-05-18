ALTER TABLE order_record
    ADD COLUMN device_id VARCHAR(128) NOT NULL DEFAULT 'unknown-device' AFTER client_type,
    ADD COLUMN channel VARCHAR(64) NOT NULL DEFAULT 'direct' AFTER device_id,
    ADD KEY idx_order_device_created (device_id, created_at),
    ADD KEY idx_order_channel_created (channel, created_at);

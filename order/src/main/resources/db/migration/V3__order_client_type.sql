ALTER TABLE order_record
    ADD COLUMN client_type VARCHAR(16) NOT NULL DEFAULT 'WEB' AFTER quantity,
    ADD KEY idx_order_client_created (client_type, created_at);

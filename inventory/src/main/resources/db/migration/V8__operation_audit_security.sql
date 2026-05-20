ALTER TABLE internal_operation_audit
    ADD COLUMN role VARCHAR(64) NULL,
    ADD COLUMN approval_id VARCHAR(128) NULL,
    ADD COLUMN source_identity VARCHAR(256) NULL,
    ADD COLUMN parameter_digest VARCHAR(512) NULL,
    ADD KEY idx_inventory_operation_approval (approval_id);

ALTER TABLE search_document
    ADD COLUMN event_version BIGINT NOT NULL DEFAULT 0 AFTER saleable,
    ADD KEY idx_search_document_version (sku_id, event_version);

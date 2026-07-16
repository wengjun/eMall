ALTER TABLE order_create_saga
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER attempts;

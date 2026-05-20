ALTER TABLE coupon
    ADD COLUMN reservation_id VARCHAR(128) NULL AFTER expires_at,
    ADD COLUMN reserved_order_id BIGINT NULL AFTER reservation_id,
    ADD KEY idx_coupon_reservation (reservation_id),
    ADD KEY idx_coupon_reserved_order (reserved_order_id);

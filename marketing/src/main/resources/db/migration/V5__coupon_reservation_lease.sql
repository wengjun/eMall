ALTER TABLE coupon
    ADD COLUMN reserved_until TIMESTAMP(6) NULL AFTER reserved_order_id,
    ADD KEY idx_coupon_reservation_expiry (status, reserved_until);

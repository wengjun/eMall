package com.emall.inventory.domain;

import java.time.Instant;

public record InventoryReservation(String requestId, long skuId, int quantity, Integer bucketNo,
        ReservationStatus status, String reason, Instant expiresAt, Instant createdAt, Instant updatedAt) {
    public InventoryReservation confirm() {
        return new InventoryReservation(requestId, skuId, quantity, bucketNo, ReservationStatus.CONFIRMED, reason,
                expiresAt, createdAt, Instant.now());
    }

    public InventoryReservation release() {
        return new InventoryReservation(requestId, skuId, quantity, bucketNo, ReservationStatus.RELEASED, reason,
                expiresAt, createdAt, Instant.now());
    }

    public static InventoryReservation reserved(String requestId, long skuId, int quantity, Integer bucketNo,
            Instant expiresAt) {
        Instant now = Instant.now();
        return new InventoryReservation(requestId, skuId, quantity, bucketNo, ReservationStatus.RESERVED, "RESERVED",
                expiresAt, now, now);
    }

    public static InventoryReservation rejected(String requestId, long skuId, int quantity, String reason) {
        Instant now = Instant.now();
        return new InventoryReservation(requestId, skuId, quantity, null, ReservationStatus.REJECTED, reason, now, now,
                now);
    }
}

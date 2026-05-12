package com.emall.common.rpc;

import java.io.Serializable;
import java.time.Instant;

public record InventoryReservationView(String requestId, long skuId, int quantity, String status, String reason,
        Instant expiresAt, Instant createdAt, Instant updatedAt) implements Serializable {
}

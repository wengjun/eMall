package com.emall.merchant.domain;

import java.time.Instant;

public record Store(
        long storeId,
        long merchantId,
        String name,
        String description,
        StoreStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public Store changeStatus(StoreStatus newStatus) {
        return new Store(storeId, merchantId, name, description, newStatus, createdAt, Instant.now());
    }
}

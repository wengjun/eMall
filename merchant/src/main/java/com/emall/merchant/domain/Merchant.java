package com.emall.merchant.domain;

import java.time.Instant;

public record Merchant(
        long merchantId,
        String name,
        String contactEmail,
        MerchantStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public Merchant changeStatus(MerchantStatus newStatus) {
        return new Merchant(merchantId, name, contactEmail, newStatus, createdAt, Instant.now());
    }
}

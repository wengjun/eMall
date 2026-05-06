package com.emall.flashsale.domain;

import java.time.Instant;

public record FlashSaleToken(
        long tokenId,
        long campaignId,
        long userId,
        long skuId,
        int quantity,
        String token,
        Instant expiresAt,
        boolean used,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public FlashSaleToken use() {
        return new FlashSaleToken(tokenId, campaignId, userId, skuId, quantity, token, expiresAt, true,
                createdAt, Instant.now());
    }
}

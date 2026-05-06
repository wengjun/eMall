package com.emall.flashsale.domain;

import java.time.Instant;

public record FlashSaleStock(
        long campaignId,
        long skuId,
        int totalStock,
        int availableStock,
        int tokenReservedStock,
        int queuedStock,
        int soldStock,
        Instant updatedAt
) {
    public FlashSaleStock reserveForToken(int quantity) {
        return new FlashSaleStock(campaignId, skuId, totalStock, availableStock - quantity,
                tokenReservedStock + quantity, queuedStock, soldStock, Instant.now());
    }

    public FlashSaleStock moveTokenToQueue(int quantity) {
        return new FlashSaleStock(campaignId, skuId, totalStock, availableStock, tokenReservedStock - quantity,
                queuedStock + quantity, soldStock, Instant.now());
    }
}

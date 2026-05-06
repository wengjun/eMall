package com.emall.flashsale.domain;

import java.time.Instant;

public record FlashSaleOrderRequest(
        long requestId,
        long campaignId,
        long userId,
        long skuId,
        int quantity,
        String token,
        FlashSaleRequestStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}

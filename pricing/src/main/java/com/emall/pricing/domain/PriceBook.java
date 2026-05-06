package com.emall.pricing.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceBook(
        long skuId,
        BigDecimal listPrice,
        BigDecimal salePrice,
        String currency,
        long version,
        boolean active,
        Instant updatedAt
) {
    public PriceBook change(BigDecimal newListPrice, BigDecimal newSalePrice, String newCurrency, boolean newActive) {
        return new PriceBook(skuId, newListPrice, newSalePrice, newCurrency, version + 1, newActive, Instant.now());
    }
}

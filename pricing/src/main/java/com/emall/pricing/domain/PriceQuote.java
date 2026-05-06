package com.emall.pricing.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceQuote(
        long skuId,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal,
        String currency,
        long priceVersion,
        Instant quotedAt
) {
    public static PriceQuote of(PriceBook priceBook, int quantity) {
        BigDecimal subtotal = priceBook.salePrice().multiply(BigDecimal.valueOf(quantity));
        return new PriceQuote(priceBook.skuId(), priceBook.salePrice(), quantity, subtotal,
                priceBook.currency(), priceBook.version(), Instant.now());
    }
}

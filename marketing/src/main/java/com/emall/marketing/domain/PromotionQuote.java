package com.emall.marketing.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionQuote(long userId, BigDecimal orderAmount, BigDecimal discountAmount, BigDecimal payableAmount,
        String couponId, Instant quotedAt) {
    public static PromotionQuote none(long userId, BigDecimal orderAmount) {
        return new PromotionQuote(userId, orderAmount, BigDecimal.ZERO, orderAmount, null, Instant.now());
    }
}

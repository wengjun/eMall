package com.emall.common.rpc;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record PromotionQuoteView(long userId, BigDecimal orderAmount, BigDecimal discountAmount,
        BigDecimal payableAmount, String couponId, Instant quotedAt) implements Serializable {
}

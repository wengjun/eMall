package com.emall.common.rpc;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record PriceQuoteView(long skuId, BigDecimal unitPrice, int quantity, BigDecimal subtotal, String currency,
        long priceVersion, Instant quotedAt) implements Serializable {
}

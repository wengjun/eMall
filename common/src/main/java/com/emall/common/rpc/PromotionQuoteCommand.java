package com.emall.common.rpc;

import java.io.Serializable;
import java.math.BigDecimal;

public record PromotionQuoteCommand(long userId, BigDecimal orderAmount) implements Serializable {
}

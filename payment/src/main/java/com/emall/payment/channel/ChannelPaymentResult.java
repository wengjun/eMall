package com.emall.payment.channel;

import java.math.BigDecimal;

public record ChannelPaymentResult(String channelTradeNo, ChannelOperationStatus status, BigDecimal amount,
        String currency, String message) {
}

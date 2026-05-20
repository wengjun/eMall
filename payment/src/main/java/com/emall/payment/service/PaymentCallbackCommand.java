package com.emall.payment.service;

import com.emall.common.privacy.SensitiveDataMasker;
import com.emall.common.privacy.SensitiveDataType;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCallbackCommand(String channel, String channelTradeNo, long paymentId, BigDecimal paidAmount,
        Instant timestamp, String nonce, String signature) {
    @Override
    public String toString() {
        return "PaymentCallbackCommand[channel=" + channel + ", channelTradeNo="
                + SensitiveDataMasker.mask(SensitiveDataType.PAYMENT_REFERENCE, channelTradeNo) + ", paymentId="
                + paymentId + ", paidAmount=" + paidAmount + ", timestamp=" + timestamp + ", nonce="
                + SensitiveDataMasker.mask(SensitiveDataType.TOKEN, nonce) + ", signature="
                + SensitiveDataMasker.mask(SensitiveDataType.TOKEN, signature) + "]";
    }
}

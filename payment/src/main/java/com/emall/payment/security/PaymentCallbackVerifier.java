package com.emall.payment.security;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.payment.service.PaymentCallbackCommand;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class PaymentCallbackVerifier {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final PaymentSecurityProperties properties;
    private final Clock clock;

    public PaymentCallbackVerifier(PaymentSecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void verify(PaymentCallbackCommand command) {
        if (!properties.isCallbackSignatureEnabled()) {
            return;
        }
        if (command.channel() == null || command.channel().isBlank() || command.channelTradeNo() == null
                || command.channelTradeNo().isBlank() || command.paymentId() <= 0 || command.paidAmount() == null
                || command.paidAmount().signum() <= 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "payment callback identity and amount are invalid");
        }
        if (command.timestamp() == null || command.nonce() == null || command.nonce().isBlank()
                || command.signature() == null || command.signature().isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "payment callback signature is required");
        }
        long skew = Math.abs(Duration.between(command.timestamp(), clock.instant()).getSeconds());
        if (skew > properties.getCallbackAllowedSkewSeconds()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "payment callback timestamp is expired");
        }
        String expected = sign(command.channel(), command.channelTradeNo(), command.paymentId(), command.paidAmount(),
                command.timestamp(), command.nonce());
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                command.signature().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "payment callback signature is invalid");
        }
    }

    public String sign(String channel, String channelTradeNo, long paymentId, BigDecimal paidAmount, Instant timestamp,
            String nonce) {
        String payload = channel + "\n" + channelTradeNo + "\n" + paymentId + "\n"
                + paidAmount.stripTrailingZeros().toPlainString() + "\n" + timestamp + "\n" + nonce;
        return hmac(channel, payload);
    }

    private String hmac(String channel, String payload) {
        String secret = properties.callbackSecret(channel);
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "payment callback secret is missing or shorter than 32 UTF-8 bytes");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign payment callback", ex);
        }
    }
}

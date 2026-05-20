package com.emall.smoke;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class PaymentCallbackSignature {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DEFAULT_SECRET = "local-dev-payment-callback-secret";

    private PaymentCallbackSignature() {
    }

    static String sign(String channel, String channelTradeNo, long paymentId, BigDecimal paidAmount, Instant timestamp,
            String nonce) {
        String secret = System.getenv().getOrDefault("EMALL_PAYMENT_CALLBACK_SECRET", DEFAULT_SECRET);
        return sign(secret, channel, channelTradeNo, paymentId, paidAmount, timestamp, nonce);
    }

    static String sign(String secret, String channel, String channelTradeNo, long paymentId, BigDecimal paidAmount,
            Instant timestamp, String nonce) {
        String payload = channel + "\n" + channelTradeNo + "\n" + paymentId + "\n"
                + paidAmount.stripTrailingZeros().toPlainString() + "\n" + timestamp + "\n" + nonce;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign payment callback", ex);
        }
    }

}

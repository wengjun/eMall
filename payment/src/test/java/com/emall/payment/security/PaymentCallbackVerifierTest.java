package com.emall.payment.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.emall.payment.service.PaymentCallbackCommand;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PaymentCallbackVerifierTest {
    private static final Instant NOW = Instant.parse("2026-05-19T00:00:00Z");

    private final PaymentSecurityProperties properties = new PaymentSecurityProperties();
    private final PaymentCallbackVerifier verifier =
            new PaymentCallbackVerifier(properties, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldAcceptValidCallbackSignature() {
        PaymentCallbackCommand command =
                signedCommand("ALIPAY", "trade-001", 90001L, new BigDecimal("100.00"), NOW, "nonce-001");

        assertThatCode(() -> verifier.verify(command)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidCallbackSignature() {
        PaymentCallbackCommand command = new PaymentCallbackCommand("ALIPAY", "trade-001", 90001L,
                new BigDecimal("100.00"), NOW, "nonce-001", "invalid-signature");

        assertThatThrownBy(() -> verifier.verify(command)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("signature is invalid");
    }

    @Test
    void shouldRejectExpiredCallbackTimestamp() {
        PaymentCallbackCommand command = signedCommand("ALIPAY", "trade-001", 90001L, new BigDecimal("100.00"),
                NOW.minusSeconds(properties.getCallbackAllowedSkewSeconds() + 1), "nonce-001");

        assertThatThrownBy(() -> verifier.verify(command)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("timestamp is expired");
    }

    @Test
    void shouldBypassSignatureWhenDisabled() {
        properties.setCallbackSignatureEnabled(false);
        PaymentCallbackCommand command =
                new PaymentCallbackCommand("ALIPAY", "trade-001", 90001L, new BigDecimal("100.00"), null, null, null);

        assertThatCode(() -> verifier.verify(command)).doesNotThrowAnyException();
    }

    private PaymentCallbackCommand signedCommand(String channel, String tradeNo, long paymentId, BigDecimal amount,
            Instant timestamp, String nonce) {
        String signature = verifier.sign(channel, tradeNo, paymentId, amount, timestamp, nonce);
        return new PaymentCallbackCommand(channel, tradeNo, paymentId, amount, timestamp, nonce, signature);
    }
}

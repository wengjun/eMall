package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CheckoutHttpRequestDispatcherTest {
    @Test
    void shouldSignCallbacksWithThePaymentServiceCanonicalPayload() {
        Map<String, String> environment = LoadTestOptionsTest.environment();
        environment.put("EMALL_LOAD_SCENARIO", "payment-callbacks");
        environment.put("EMALL_LOAD_PAYMENT_CALLBACK_SECRET", "0123456789abcdef0123456789abcdef");
        CheckoutHttpRequestDispatcher dispatcher =
                new CheckoutHttpRequestDispatcher(LoadTestOptions.from(new String[0], environment));

        String signature = dispatcher.signPaymentCallback("loadtest", "loadtest-trade-7", 800_000_007L,
                new BigDecimal("3799.00"), Instant.parse("2026-01-01T00:00:00Z"), "nonce-7");

        assertThat(signature).isEqualTo("HeSh1yzybZu0atgaANJ03YfvcgU6m-qc9RO6TBasmfg");
    }

    @Test
    void shouldRefuseUnsignedPaymentCallbackTraffic() {
        Map<String, String> environment = LoadTestOptionsTest.environment();
        environment.put("EMALL_LOAD_SCENARIO", "payment-callbacks");

        assertThatThrownBy(() -> new CheckoutHttpRequestDispatcher(LoadTestOptions.from(new String[0], environment)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least 32");
    }
}

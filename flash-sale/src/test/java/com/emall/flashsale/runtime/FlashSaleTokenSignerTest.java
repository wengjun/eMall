package com.emall.flashsale.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FlashSaleTokenSignerTest {
    private final FlashSaleTokenSigner signer = new FlashSaleTokenSigner("flash-sale-secret");

    @Test
    void shouldVerifyTokenCreatedBySameSecret() {
        String token = signer.sign(10001L, 30001L, 70001L, 90001L, 2, Instant.parse("2026-05-19T00:05:00Z"));

        assertThat(signer.verify(token)).isTrue();
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = signer.sign(10001L, 30001L, 70001L, 90001L, 2, Instant.parse("2026-05-19T00:05:00Z"));

        assertThat(signer.verify(token + "x")).isFalse();
        assertThat(new FlashSaleTokenSigner("another-secret").verify(token)).isFalse();
    }
}

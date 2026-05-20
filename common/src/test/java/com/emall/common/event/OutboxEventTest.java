package com.emall.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OutboxEventTest {
    @Test
    void shouldMoveThroughClaimPublishAndRetryStates() {
        OutboxEvent created =
                OutboxEvent.create("event-001", "Order", "70001", EventTypes.ORDER_CREATED, Map.of("orderId", 70001L));
        Instant claimUntil = Instant.parse("2026-05-19T00:01:00Z");

        OutboxEvent claimed = created.claimed("publisher-1", claimUntil);
        OutboxEvent failed =
                claimed.failed(Instant.parse("2026-05-19T00:02:00Z"), "PUBLISH_FAILED", "broker unavailable");
        OutboxEvent retryReady = failed.readyForRetry(Instant.parse("2026-05-19T00:03:00Z"));
        OutboxEvent published = retryReady.claimed("publisher-2", claimUntil.plusSeconds(60)).published();

        assertThat(created.status()).isEqualTo(OutboxStatus.NEW);
        assertThat(claimed.status()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(claimed.claimedBy()).isEqualTo("publisher-1");
        assertThat(failed.status()).isEqualTo(OutboxStatus.FAILED);
        assertThat(failed.retryCount()).isOne();
        assertThat(retryReady.nextRetryAt()).isEqualTo(Instant.parse("2026-05-19T00:03:00Z"));
        assertThat(published.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(published.claimedBy()).isNull();
        assertThat(published.errorCode()).isNull();
    }

    @Test
    void shouldTruncateLongErrorMessageWhenDead() {
        OutboxEvent event = OutboxEvent.create("event-002", "Payment", "90001", EventTypes.PAYMENT_SUCCEEDED,
                Map.of("paymentId", 90001L));

        OutboxEvent dead = event.dead("PUBLISH_FAILED", "x".repeat(700));

        assertThat(dead.status()).isEqualTo(OutboxStatus.DEAD);
        assertThat(dead.lastError()).hasSize(512);
    }
}

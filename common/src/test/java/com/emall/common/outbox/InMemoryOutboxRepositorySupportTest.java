package com.emall.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryOutboxRepositorySupportTest {
    @Test
    void shouldClaimPublishableEventsWithoutDuplicateBatches() {
        TestOutboxRepository repository = new TestOutboxRepository();
        for (int index = 0; index < 5; index++) {
            repository.save(OutboxEvent.create("event-" + index, "Order", "order-" + index, EventTypes.ORDER_CREATED,
                    Map.of("orderId", index)));
        }

        Instant now = Instant.now().plusSeconds(1);
        var firstBatch = repository.claimPublishable("owner-a", now, Duration.ofSeconds(30), 3);
        var secondBatch = repository.claimPublishable("owner-b", now, Duration.ofSeconds(30), 3);

        assertThat(firstBatch).hasSize(3);
        assertThat(secondBatch).hasSize(2);
        assertThat(firstBatch).extracting(OutboxEvent::eventId)
                .doesNotContainAnyElementsOf(secondBatch.stream().map(OutboxEvent::eventId).toList());
        assertThat(firstBatch).allSatisfy(event -> {
            assertThat(event.status()).isEqualTo(OutboxStatus.PROCESSING);
            assertThat(event.claimedBy()).isEqualTo("owner-a");
        });
        assertThat(secondBatch).allSatisfy(event -> assertThat(event.claimedBy()).isEqualTo("owner-b"));
    }

    @Test
    void shouldReclaimExpiredProcessingLease() {
        TestOutboxRepository repository = new TestOutboxRepository();
        repository.save(
                OutboxEvent.create("event-001", "Order", "10001", EventTypes.ORDER_CREATED, Map.of("orderId", 10001L)));

        Instant now = Instant.now().plusSeconds(1);
        repository.claimPublishable("owner-a", now, Duration.ofSeconds(5), 1);
        var reclaimed = repository.claimPublishable("owner-b", now.plusSeconds(6), Duration.ofSeconds(5), 1);

        assertThat(reclaimed).singleElement().satisfies(event -> {
            assertThat(event.status()).isEqualTo(OutboxStatus.PROCESSING);
            assertThat(event.claimedBy()).isEqualTo("owner-b");
        });
    }

    private static final class TestOutboxRepository extends InMemoryOutboxRepositorySupport {
    }
}

package com.emall.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OrderEventPayload;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

class InMemoryOutboxRepositorySupportTest {
    @Test
    void shouldClaimPublishableEventsWithoutDuplicateBatches() {
        TestOutboxRepository repository = new TestOutboxRepository();
        for (int index = 0; index < 5; index++) {
            repository.save(orderEvent("event-" + index, "order-" + index, index));
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
        repository.save(orderEvent("event-001", "10001", 10001L));

        Instant now = Instant.now().plusSeconds(1);
        repository.claimPublishable("owner-a", now, Duration.ofSeconds(5), 1);
        var reclaimed = repository.claimPublishable("owner-b", now.plusSeconds(6), Duration.ofSeconds(5), 1);

        assertThat(reclaimed).singleElement().satisfies(event -> {
            assertThat(event.status()).isEqualTo(OutboxStatus.PROCESSING);
            assertThat(event.claimedBy()).isEqualTo("owner-b");
        });
    }

    @Test
    void shouldAllocateVersionsAndPublishOneAggregateEventAtATime() {
        TestOutboxRepository repository = new TestOutboxRepository();
        OutboxEvent first = repository.save(orderEvent("event-001", "10001", 10001L));
        OutboxEvent duplicate = repository.save(orderEvent("event-001", "10001", 10001L));
        OutboxEvent second = repository.save(orderEvent("event-002", "10001", 10001L));
        Instant now = Instant.now().plusSeconds(1);

        assertThat(first.aggregateVersion()).isOne();
        assertThat(duplicate.aggregateVersion()).isOne();
        assertThat(second.aggregateVersion()).isEqualTo(2);
        OutboxEvent claimedFirst = repository.claimPublishable("owner-a", now, Duration.ofSeconds(30), 10).get(0);
        assertThat(claimedFirst.eventId()).isEqualTo("event-001");

        repository.save(claimedFirst.failed(now));
        assertThat(repository.claimPublishable("owner-b", now.plusMillis(1), Duration.ofSeconds(30), 10))
                .singleElement().extracting(OutboxEvent::eventId).isEqualTo("event-001");
        repository.save(repository.claimPublishable("owner-b", now.plusSeconds(31), Duration.ofSeconds(30), 10).get(0)
                .published());

        assertThat(repository.claimPublishable("owner-c", now.plusSeconds(32), Duration.ofSeconds(30), 10))
                .singleElement().extracting(OutboxEvent::eventId).isEqualTo("event-002");
    }

    @Test
    void concurrentDuplicateEventMustNotConsumeAggregateVersions() {
        TestOutboxRepository repository = new TestOutboxRepository();
        ExecutorService executor = Executors.newFixedThreadPool(12);
        try {
            List<CompletableFuture<OutboxEvent>> saves =
                    IntStream.range(0, 100)
                            .mapToObj(ignored -> CompletableFuture.supplyAsync(
                                    () -> repository.save(orderEvent("event-duplicate", "10001", 10001L)), executor))
                            .toList();
            CompletableFuture.allOf(saves.toArray(CompletableFuture[]::new)).join();

            assertThat(saves).allSatisfy(save -> assertThat(save.join().aggregateVersion()).isOne());
            assertThat(repository.save(orderEvent("event-next", "10001", 10001L)).aggregateVersion()).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    private static OutboxEvent orderEvent(String eventId, String aggregateId, long orderId) {
        OrderEventPayload payload =
                new OrderEventPayload(orderId, 2001L, 3001L, 1, "WEB", "device-1", "direct", BigDecimal.TEN,
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, "CNY", 1L, "", "reservation-1", "CREATED");
        return OutboxEvent.create(eventId, "Order", aggregateId, EventTypes.ORDER_CREATED, "order", "1.0.0", payload);
    }

    private static final class TestOutboxRepository extends InMemoryOutboxRepositorySupport {
    }
}

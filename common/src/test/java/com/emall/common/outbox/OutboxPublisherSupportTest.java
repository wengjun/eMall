package com.emall.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
import com.emall.common.task.DistributedTaskLock;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class OutboxPublisherSupportTest {
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final TestOutboxRepository repository = new TestOutboxRepository();
    private final TestPublisher publisher = new TestPublisher(repository, kafkaTemplate);

    @Test
    void shouldMarkEventPublishedAfterKafkaSuccess() {
        @SuppressWarnings("unchecked")
        SendResult<String, String> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        OutboxEvent event =
                OutboxEvent.create("event-001", "Order", "70001", EventTypes.ORDER_CREATED, Map.of("orderId", 70001L));
        repository.save(event);

        int published = publisher.publishBatch(10);

        assertThat(published).isOne();
        assertThat(repository.saved("event-001").status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(repository.saved("event-001").publishedAt()).isNotNull();
    }

    @Test
    void shouldMarkEventFailedAfterKafkaFailure() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("kafka unavailable")));
        OutboxEvent event =
                OutboxEvent.create("event-002", "Order", "70002", EventTypes.ORDER_CREATED, Map.of("orderId", 70002L));
        repository.save(event);

        int published = publisher.publishBatch(10);

        assertThat(published).isZero();
        assertThat(repository.saved("event-002").status()).isEqualTo(OutboxStatus.FAILED);
        assertThat(repository.saved("event-002").retryCount()).isOne();
        assertThat(repository.saved("event-002").lastError()).contains("kafka unavailable");
    }

    private static final class TestPublisher extends OutboxPublisherSupport {
        private TestPublisher(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
            super(repository, kafkaTemplate, new ObjectMapper().registerModule(new JavaTimeModule()), "order",
                    "order-events", mock(DistributedTaskLock.class));
        }
    }

    private static final class TestOutboxRepository implements OutboxRepository {
        private final ConcurrentMap<String, OutboxEvent> events = new ConcurrentHashMap<>();

        @Override
        public OutboxEvent save(OutboxEvent event) {
            events.put(event.eventId(), event);
            return event;
        }

        @Override
        public List<OutboxEvent> claimPublishable(String ownerId, Instant now, Duration leaseTtl, int limit) {
            return events.values().stream().filter(event -> event.status() == OutboxStatus.NEW).limit(limit)
                    .map(event -> event.claimed(ownerId, now.plus(leaseTtl))).peek(this::save).toList();
        }

        @Override
        public List<OutboxEvent> findPublishable(Instant now, int limit) {
            return events.values().stream().filter(event -> event.status() == OutboxStatus.NEW).limit(limit).toList();
        }

        @Override
        public int rescheduleFailed(Instant now, int limit) {
            return 0;
        }

        private OutboxEvent saved(String eventId) {
            return events.get(eventId);
        }
    }
}

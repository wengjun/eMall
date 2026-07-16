package com.emall.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OrderEventPayload;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
import com.emall.common.task.DistributedTaskLock;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
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
        OutboxEvent event = orderEvent("event-001", "70001", 70001L);
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
        OutboxEvent event = orderEvent("event-002", "70002", 70002L);
        repository.save(event);

        int published = publisher.publishBatch(10);

        assertThat(published).isZero();
        assertThat(repository.saved("event-002").status()).isEqualTo(OutboxStatus.FAILED);
        assertThat(repository.saved("event-002").retryCount()).isOne();
        assertThat(repository.saved("event-002").lastError()).contains("kafka unavailable");
    }

    @Test
    void shouldDeadLetterInvalidContractBeforeSendingToKafka() {
        OutboxEvent malformed = OutboxEvent.create("event-invalid", "Order", "70003", EventTypes.ORDER_CREATED,
                Map.of("unexpected", true));
        repository.save(malformed);

        assertThat(publisher.publishBatch(10)).isZero();
        assertThat(repository.saved("event-invalid").status()).isEqualTo(OutboxStatus.DEAD);
        assertThat(repository.saved("event-invalid").errorCode()).isEqualTo("CONTRACT_INVALID");
        verifyNoInteractions(kafkaTemplate);
    }

    private OutboxEvent orderEvent(String eventId, String aggregateId, long orderId) {
        OrderEventPayload payload =
                new OrderEventPayload(orderId, 10001L, 30001L, 1, "WEB", "device-1", "direct", BigDecimal.TEN,
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, "CNY", 1L, "", "reservation-1", "CREATED");
        return OutboxEvent.create(eventId, "Order", aggregateId, EventTypes.ORDER_CREATED, "order", "1.0.0", payload);
    }

    private static final class TestPublisher extends OutboxPublisherSupport {
        private TestPublisher(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
            super(repository, kafkaTemplate, new ObjectMapper().registerModule(new JavaTimeModule()), "order",
                    "order-events", mock(DistributedTaskLock.class));
        }
    }

    private static final class TestOutboxRepository extends InMemoryOutboxRepositorySupport {
        private final ConcurrentMap<String, OutboxEvent> events = new ConcurrentHashMap<>();

        @Override
        public OutboxEvent save(OutboxEvent event) {
            OutboxEvent persisted = super.save(event);
            events.put(persisted.eventId(), persisted);
            return persisted;
        }

        private OutboxEvent saved(String eventId) {
            return events.get(eventId);
        }
    }
}

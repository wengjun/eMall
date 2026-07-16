package com.emall.common.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OrderEventPayload;
import com.emall.common.event.OutboxEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import com.emall.common.metrics.BusinessMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MessageConsumerTemplateTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final InMemoryProcessedMessageRepository repository = new InMemoryProcessedMessageRepository();
    private final MessageConsumerTemplate template =
            new MessageConsumerTemplate(objectMapper, repository, BusinessMetrics.noop(), 2, "test-consumer");

    @Test
    void shouldProcessFirstDeliveryAndSkipDuplicateDelivery() throws Exception {
        String message = message(EventTypes.ORDER_PAID);
        AtomicInteger handled = new AtomicInteger();

        ConsumerExecutionResult first =
                template.consume(message, EventTypes.ORDER_PAID, event -> handled.incrementAndGet());
        ConsumerExecutionResult duplicate =
                template.consume(message, EventTypes.ORDER_PAID, event -> handled.incrementAndGet());

        assertThat(first).isEqualTo(ConsumerExecutionResult.PROCESSED);
        assertThat(duplicate).isEqualTo(ConsumerExecutionResult.DUPLICATED);
        assertThat(handled).hasValue(1);
    }

    @Test
    void shouldIgnoreUnexpectedEventTypeWithoutClaimingMessage() throws Exception {
        AtomicInteger handled = new AtomicInteger();

        ConsumerExecutionResult result = template.consume(message(EventTypes.ORDER_CREATED), EventTypes.ORDER_PAID,
                event -> handled.incrementAndGet());

        assertThat(result).isEqualTo(ConsumerExecutionResult.IGNORED);
        assertThat(handled).hasValue(0);
    }

    @Test
    void shouldRetryFailedMessageAndStopAfterDeadLetterThreshold() throws Exception {
        String message = message(EventTypes.ORDER_PAID);

        assertThatThrownBy(() -> template.consume(message, EventTypes.ORDER_PAID, event -> {
            throw new IllegalStateException("downstream unavailable");
        })).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> template.consume(message, EventTypes.ORDER_PAID, event -> {
            throw new IllegalStateException("downstream unavailable");
        })).isInstanceOf(DeadMessageException.class);

        assertThat(template.consume(message, EventTypes.ORDER_PAID, event -> {
        })).isEqualTo(ConsumerExecutionResult.DUPLICATED);
    }

    @Test
    void shouldIgnoreStaleAggregateVersionWithoutInvokingHandler() {
        AtomicInteger handled = new AtomicInteger();
        OutboxEvent newest = event("event-1002", 2);
        OutboxEvent stale = event("event-1001", 1);

        ConsumerExecutionResult newestResult =
                template.consume(newest, EventTypes.ORDER_PAID, event -> handled.incrementAndGet());
        ConsumerExecutionResult staleResult =
                template.consume(stale, EventTypes.ORDER_PAID, event -> handled.incrementAndGet());

        assertThat(newestResult).isEqualTo(ConsumerExecutionResult.PROCESSED);
        assertThat(staleResult).isEqualTo(ConsumerExecutionResult.STALE);
        assertThat(handled).hasValue(1);
    }

    @Test
    void shouldRetryAnEventWhenAggregateVersionHasGap() {
        AtomicInteger handled = new AtomicInteger();
        template.consume(event("event-1001", 1), EventTypes.ORDER_PAID, event -> handled.incrementAndGet());

        assertThatThrownBy(() -> template.consume(event("event-1003", 3), EventTypes.ORDER_PAID,
                event -> handled.incrementAndGet())).isInstanceOf(EventVersionGapException.class)
                .hasMessageContaining("current=1, requested=3");
        assertThat(handled).hasValue(1);
    }

    @Test
    void shouldRejectMalformedContractBeforeInvokingHandler() {
        Instant now = Instant.parse("2026-07-15T00:00:00Z");
        OutboxEvent malformed = new OutboxEvent("event-invalid", "Order", "1001", EventTypes.ORDER_PAID, 1, 1, "order",
                "1.0.0", now, null, null, Map.of("unexpected", true), com.emall.common.event.OutboxStatus.NEW, 0, now,
                now, now, 1, null, null, null, null, null);
        AtomicInteger handled = new AtomicInteger();

        assertThatThrownBy(() -> template.consume(malformed, EventTypes.ORDER_PAID, event -> handled.incrementAndGet()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("orderId");
        assertThat(handled).hasValue(0);
    }

    private String message(String eventType) throws Exception {
        OutboxEvent event = event("event-1001", 1, eventType);
        return objectMapper.writeValueAsString(event);
    }

    private OutboxEvent event(String eventId, long aggregateVersion) {
        return event(eventId, aggregateVersion, EventTypes.ORDER_PAID);
    }

    private OutboxEvent event(String eventId, long aggregateVersion, String eventType) {
        OrderEventPayload payload =
                new OrderEventPayload(1001L, 2001L, 3001L, 1, "WEB", "device-1", "direct", BigDecimal.TEN,
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, "CNY", 1L, "", "reservation-1", "PAID");
        return OutboxEvent.create(eventId, "Order", "1001", eventType, "order", "1.0.0", payload)
                .withAggregateVersion(aggregateVersion);
    }
}

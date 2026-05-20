package com.emall.common.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.metrics.BusinessMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
        })).isInstanceOf(IllegalStateException.class);

        assertThat(template.consume(message, EventTypes.ORDER_PAID, event -> {
        })).isEqualTo(ConsumerExecutionResult.DUPLICATED);
    }

    private String message(String eventType) throws Exception {
        OutboxEvent event = OutboxEvent.create("event-1001", "Order", "1001", eventType, Map.of("orderId", 1001L));
        return objectMapper.writeValueAsString(event);
    }
}

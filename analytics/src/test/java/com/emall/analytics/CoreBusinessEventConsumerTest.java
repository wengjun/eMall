package com.emall.analytics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OrderEventPayload;
import com.emall.common.event.OutboxEvent;
import com.emall.common.messaging.InMemoryProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CoreBusinessEventConsumerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AnalyticsService analyticsService = mock(AnalyticsService.class);
    private final CoreBusinessEventConsumer consumer = new CoreBusinessEventConsumer(objectMapper, analyticsService,
            BusinessMetrics.noop(), new InMemoryProcessedMessageRepository(), 2);

    @Test
    void shouldRecordCoreBusinessEventOnceForDuplicateDelivery() throws Exception {
        String message = message();

        consumer.onCoreEvent(message);
        consumer.onCoreEvent(message);

        verify(analyticsService, times(1)).recordBusinessEvent(any(OutboxEvent.class));
    }

    private String message() throws Exception {
        OrderEventPayload payload =
                new OrderEventPayload(90001L, 10001L, 30001L, 1, "WEB", "device-1", "direct", BigDecimal.TEN,
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, "CNY", 1L, "", "reservation-001", "PAID");
        OutboxEvent event = OutboxEvent
                .create("order-event-90001", "Order", "90001", EventTypes.ORDER_PAID, "order", "1.0.0", payload)
                .withAggregateVersion(1);
        return objectMapper.writeValueAsString(event);
    }
}

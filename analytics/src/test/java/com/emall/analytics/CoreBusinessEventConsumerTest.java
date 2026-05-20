package com.emall.analytics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.messaging.InMemoryProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
        OutboxEvent event = OutboxEvent.create("order-event-90001", "Order", "90001", EventTypes.ORDER_PAID,
                Map.of("orderId", 90001L));
        return objectMapper.writeValueAsString(event);
    }
}

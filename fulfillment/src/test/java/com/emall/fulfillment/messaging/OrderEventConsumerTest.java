package com.emall.fulfillment.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OrderEventPayload;
import com.emall.common.event.OutboxEvent;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.fulfillment.repository.InMemoryProcessedMessageRepository;
import com.emall.fulfillment.service.FulfillmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderEventConsumerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final FulfillmentService fulfillmentService = mock(FulfillmentService.class);
    private final OrderEventConsumer consumer = new OrderEventConsumer(objectMapper, fulfillmentService,
            new InMemoryProcessedMessageRepository(), BusinessMetrics.noop(), 2, "WH-001");

    @Test
    void shouldAllocateFulfillmentOnceForDuplicatePaidOrder() throws Exception {
        String message = message();

        consumer.onOrderEvent(message);
        consumer.onOrderEvent(message);

        verify(fulfillmentService, times(1)).allocate(90001L, 10001L, 30001L, 2, "WH-001");
    }

    private String message() throws Exception {
        OrderEventPayload payload = new OrderEventPayload(90001L, 10001L, 30001L, 2, "APP", "device-1", "direct",
                BigDecimal.TEN, new BigDecimal("20.00"), BigDecimal.ZERO, new BigDecimal("20.00"), "CNY", 1L, "",
                "reservation-001", "PAID");
        OutboxEvent event = OutboxEvent
                .create("order-event-90001", "Order", "90001", EventTypes.ORDER_PAID, "order", "1.0.0", payload)
                .withAggregateVersion(1);
        return objectMapper.writeValueAsString(event);
    }
}

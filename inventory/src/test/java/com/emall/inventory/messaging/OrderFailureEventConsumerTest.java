package com.emall.inventory.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OrderEventPayload;
import com.emall.common.event.OutboxEvent;
import com.emall.common.messaging.InMemoryProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderFailureEventConsumerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final InventoryService inventoryService = mock(InventoryService.class);
    private final OrderFailureEventConsumer consumer = new OrderFailureEventConsumer(objectMapper, inventoryService,
            BusinessMetrics.noop(), new InMemoryProcessedMessageRepository(), 2);

    @Test
    void shouldReleaseInventoryReservationOnceForDuplicatedOrderCancelledEvent() throws Exception {
        String message = message();

        consumer.onOrderEvent(message);
        consumer.onOrderEvent(message);

        verify(inventoryService, times(1)).release("reservation-001");
    }

    private String message() throws Exception {
        OrderEventPayload payload =
                new OrderEventPayload(90001L, 10001L, 30001L, 1, "WEB", "device-1", "direct", BigDecimal.TEN,
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, "CNY", 1L, "", "reservation-001", "CANCELLED");
        OutboxEvent event = OutboxEvent
                .create("order-event-90001", "Order", "90001", EventTypes.ORDER_CANCELLED, "order", "1.0.0", payload)
                .withAggregateVersion(1);
        return objectMapper.writeValueAsString(event);
    }
}

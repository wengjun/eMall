package com.emall.datawarehouse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.emall.common.event.EventTypes;
import com.emall.common.event.InventoryReservationEventPayload;
import com.emall.common.event.OutboxEvent;
import com.emall.common.messaging.InMemoryProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BusinessEventConsumerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final DataWarehouseService dataWarehouseService = mock(DataWarehouseService.class);
    private final BusinessEventConsumer consumer = new BusinessEventConsumer(objectMapper, dataWarehouseService,
            BusinessMetrics.noop(), new InMemoryProcessedMessageRepository(), 2);

    @Test
    void shouldRecordBusinessEventOnceForDuplicateDelivery() throws Exception {
        String message = message();

        consumer.onBusinessEvent(message);
        consumer.onBusinessEvent(message);

        verify(dataWarehouseService, times(1)).recordBusinessEvent(any(OutboxEvent.class));
    }

    private String message() throws Exception {
        InventoryReservationEventPayload payload =
                new InventoryReservationEventPayload("reservation-001", 30001L, 1, 0, "RELEASED");
        OutboxEvent event = OutboxEvent.create("inventory-event-001", "InventoryReservation", "reservation-001",
                EventTypes.INVENTORY_RELEASED, "inventory", "1.0.0", payload).withAggregateVersion(1);
        return objectMapper.writeValueAsString(event);
    }
}

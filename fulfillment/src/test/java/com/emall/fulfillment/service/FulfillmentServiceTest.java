package com.emall.fulfillment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.fulfillment.domain.FulfillmentOrder;
import com.emall.fulfillment.domain.ShipmentStatus;
import com.emall.fulfillment.domain.TrackingEvent;
import com.emall.fulfillment.repository.InMemoryFulfillmentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class FulfillmentServiceTest {
    private final InMemoryFulfillmentRepository repository = new InMemoryFulfillmentRepository();
    private final FulfillmentService service = new FulfillmentService(repository, new SnowflakeIdGenerator(9L));

    @Test
    void allocatesWithBestWarehouseAndCarrierRouteThenAppliesTrackingEvents() {
        service.upsertWarehouse("WH-NORTH-2", "NORTH", 20, 20_000, true);
        service.upsertWarehouse("WH-NORTH-1", "NORTH", 10, 20_000, true);
        service.upsertWarehouse("WH-SOUTH-1", "SOUTH", 1, 20_000, true);
        service.createCarrierRoute("SF", "WH-NORTH-1", "NORTH", 20, new BigDecimal("18.00"), 48);
        service.createCarrierRoute("JD", "WH-NORTH-1", "NORTH", 10, new BigDecimal("20.00"), 24);

        FulfillmentOrder order = service.allocateWithPlan(1001L, 2001L, 3001L, 2, "NORTH");

        assertThat(order.warehouseCode()).isEqualTo("WH-NORTH-1");
        assertThat(order.plannedCarrier()).isEqualTo("JD");
        assertThat(order.estimatedSlaHours()).isEqualTo(24);

        TrackingEvent shipped = service.ingestTrackingEvent(order.fulfillmentId(), "JD", "JD1001", "SHIPPED",
                Instant.parse("2026-04-28T10:00:00Z"), "Beijing", "picked up");
        service.ingestTrackingEvent(order.fulfillmentId(), "JD", "JD1001", "DELIVERED",
                Instant.parse("2026-04-29T10:00:00Z"), "Beijing", "delivered");

        FulfillmentOrder delivered = service.get(order.fulfillmentId());
        List<TrackingEvent> events = service.findTrackingEvents(order.fulfillmentId());

        assertThat(shipped.eventCode()).isEqualTo("SHIPPED");
        assertThat(delivered.status()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(delivered.carrier()).isEqualTo("JD");
        assertThat(delivered.trackingNo()).isEqualTo("JD1001");
        assertThat(events).hasSize(2);
    }
}

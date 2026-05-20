package com.emall.fulfillment.domain;

import java.time.Instant;

public record FulfillmentOrder(long fulfillmentId, long orderId, long userId, long skuId, int quantity,
        String destinationRegionCode, String warehouseCode, String plannedCarrier, int estimatedSlaHours,
        String carrier, String trackingNo, ShipmentStatus status, Instant createdAt, Instant updatedAt) {
    public static FulfillmentOrder allocated(long fulfillmentId, long orderId, long userId, long skuId, int quantity,
            String destinationRegionCode, String warehouseCode, String plannedCarrier, int estimatedSlaHours,
            Instant now) {
        return new FulfillmentOrder(fulfillmentId, orderId, userId, skuId, quantity, destinationRegionCode,
                warehouseCode, plannedCarrier, estimatedSlaHours, null, null, ShipmentStatus.ALLOCATED, now, now);
    }

    public FulfillmentOrder ship(String newCarrier, String newTrackingNo) {
        FulfillmentStateMachine.requireTransition(status, ShipmentStatus.SHIPPED);
        return new FulfillmentOrder(fulfillmentId, orderId, userId, skuId, quantity, destinationRegionCode,
                warehouseCode, plannedCarrier, estimatedSlaHours, newCarrier, newTrackingNo, ShipmentStatus.SHIPPED,
                createdAt, Instant.now());
    }

    public FulfillmentOrder deliver() {
        FulfillmentStateMachine.requireTransition(status, ShipmentStatus.DELIVERED);
        return new FulfillmentOrder(fulfillmentId, orderId, userId, skuId, quantity, destinationRegionCode,
                warehouseCode, plannedCarrier, estimatedSlaHours, carrier, trackingNo, ShipmentStatus.DELIVERED,
                createdAt, Instant.now());
    }
}

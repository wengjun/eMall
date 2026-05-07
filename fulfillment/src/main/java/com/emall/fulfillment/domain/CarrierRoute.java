package com.emall.fulfillment.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record CarrierRoute(long routeId, String carrierCode, String originWarehouseCode, String destinationRegionCode,
        int priority, BigDecimal baseCost, int slaHours, boolean active, Instant createdAt, Instant updatedAt) {
    public CarrierRoute activate() {
        return new CarrierRoute(routeId, carrierCode, originWarehouseCode, destinationRegionCode, priority, baseCost,
                slaHours, true, createdAt, Instant.now());
    }

    public CarrierRoute deactivate() {
        return new CarrierRoute(routeId, carrierCode, originWarehouseCode, destinationRegionCode, priority, baseCost,
                slaHours, false, createdAt, Instant.now());
    }
}

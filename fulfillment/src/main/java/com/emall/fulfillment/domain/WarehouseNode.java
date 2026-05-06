package com.emall.fulfillment.domain;

import java.time.Instant;

public record WarehouseNode(
        String warehouseCode,
        String regionCode,
        int priority,
        int dailyCapacity,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public WarehouseNode enable() {
        return new WarehouseNode(warehouseCode, regionCode, priority, dailyCapacity, true, createdAt, Instant.now());
    }

    public WarehouseNode disable() {
        return new WarehouseNode(warehouseCode, regionCode, priority, dailyCapacity, false, createdAt, Instant.now());
    }
}

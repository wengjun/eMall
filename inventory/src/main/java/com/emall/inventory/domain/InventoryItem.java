package com.emall.inventory.domain;

import java.time.Instant;

public record InventoryItem(long skuId, long total, long reserved, long sold, InventoryMode mode, long version,
        Instant updatedAt) {
    public InventoryItem(long skuId, long total, long reserved, long sold, Instant updatedAt) {
        this(skuId, total, reserved, sold, InventoryMode.SINGLE_ROW, 0, updatedAt);
    }

    public long available() {
        return total - reserved - sold;
    }

    public InventoryItem add(long quantity) {
        return new InventoryItem(skuId, total + quantity, reserved, sold, mode, version + 1, Instant.now());
    }

    public InventoryItem reserve(int quantity) {
        if (available() < quantity) {
            throw new IllegalStateException("insufficient stock");
        }
        return new InventoryItem(skuId, total, reserved + quantity, sold, mode, version + 1, Instant.now());
    }

    public InventoryItem confirm(int quantity) {
        if (reserved < quantity) {
            throw new IllegalStateException("insufficient reserved stock");
        }
        return new InventoryItem(skuId, total, reserved - quantity, sold + quantity, mode, version + 1, Instant.now());
    }

    public InventoryItem release(int quantity) {
        if (reserved < quantity) {
            throw new IllegalStateException("insufficient reserved stock");
        }
        return new InventoryItem(skuId, total, reserved - quantity, sold, mode, version + 1, Instant.now());
    }

    public InventoryItem activateBuckets() {
        return new InventoryItem(skuId, reserved + sold, reserved, sold, InventoryMode.BUCKETED, version + 1,
                Instant.now());
    }

    public InventoryItem aggregate(InventoryStockSummary summary) {
        if (mode != InventoryMode.BUCKETED || summary.bucketCount() == 0) {
            return this;
        }
        Instant aggregateUpdatedAt =
                summary.updatedAt() != null && summary.updatedAt().isAfter(updatedAt) ? summary.updatedAt() : updatedAt;
        return new InventoryItem(skuId, total + summary.total(), reserved + summary.reserved(), sold + summary.sold(),
                mode, version, aggregateUpdatedAt);
    }
}

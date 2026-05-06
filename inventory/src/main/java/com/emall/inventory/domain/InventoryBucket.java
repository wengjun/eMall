package com.emall.inventory.domain;

import java.time.Instant;

public record InventoryBucket(
        long skuId,
        int bucketNo,
        int total,
        int reserved,
        int sold,
        Instant updatedAt
) {
    public int available() {
        return total - reserved - sold;
    }

    public InventoryBucket add(int quantity) {
        return new InventoryBucket(skuId, bucketNo, total + quantity, reserved, sold, Instant.now());
    }

    public InventoryBucket reserve(int quantity) {
        if (available() < quantity) {
            throw new IllegalStateException("insufficient bucket stock");
        }
        return new InventoryBucket(skuId, bucketNo, total, reserved + quantity, sold, Instant.now());
    }

    public InventoryBucket confirm(int quantity) {
        if (reserved < quantity) {
            throw new IllegalStateException("insufficient reserved bucket stock");
        }
        return new InventoryBucket(skuId, bucketNo, total, reserved - quantity, sold + quantity, Instant.now());
    }

    public InventoryBucket release(int quantity) {
        if (reserved < quantity) {
            throw new IllegalStateException("insufficient reserved bucket stock");
        }
        return new InventoryBucket(skuId, bucketNo, total, reserved - quantity, sold, Instant.now());
    }
}

package com.emall.inventory.domain;

import java.time.Instant;

public record InventoryItem(long skuId, long total, long reserved, long sold, Instant updatedAt) {
    public long available() {
        return total - reserved - sold;
    }

    public InventoryItem add(long quantity) {
        return new InventoryItem(skuId, total + quantity, reserved, sold, Instant.now());
    }

    public InventoryItem reserve(int quantity) {
        if (available() < quantity) {
            throw new IllegalStateException("insufficient stock");
        }
        return new InventoryItem(skuId, total, reserved + quantity, sold, Instant.now());
    }

    public InventoryItem confirm(int quantity) {
        if (reserved < quantity) {
            throw new IllegalStateException("insufficient reserved stock");
        }
        return new InventoryItem(skuId, total, reserved - quantity, sold + quantity, Instant.now());
    }

    public InventoryItem release(int quantity) {
        if (reserved < quantity) {
            throw new IllegalStateException("insufficient reserved stock");
        }
        return new InventoryItem(skuId, total, reserved - quantity, sold, Instant.now());
    }
}

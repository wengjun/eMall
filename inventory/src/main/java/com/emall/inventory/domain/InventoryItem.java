package com.emall.inventory.domain;

import java.time.Instant;

public record InventoryItem(long skuId, int total, int reserved, int sold, Instant updatedAt) {
    public int available() {
        return total - reserved - sold;
    }

    public InventoryItem add(int quantity) {
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

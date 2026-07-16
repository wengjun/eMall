package com.emall.inventory.domain;

import java.time.Instant;

public record InventoryStockSummary(long total, long reserved, long sold, int bucketCount, Instant updatedAt) {
}

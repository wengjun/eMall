package com.emall.inventory.domain;

import java.time.Instant;

public record InventoryStockLedger(String ledgerId, long skuId, String requestId, InventoryLedgerOperation operation,
        Integer bucketNo, long totalDelta, long reservedDelta, long soldDelta, Instant createdAt) {
}

package com.emall.supplychain;

import java.time.Instant;
import java.time.LocalDate;

enum ReceiptStatus {
    CREATED,
    RECEIVED,
    SHELVED
}

enum TransferStatus {
    CREATED,
    IN_TRANSIT,
    COMPLETED,
    CANCELLED
}

enum WaybillStatus {
    CREATED,
    IN_TRANSIT,
    EXCEPTION,
    DELIVERED
}

record WarehouseReceipt(long receiptId, long skuId, String warehouseCode, String batchNo, int quantity,
        LocalDate expiresOn, ReceiptStatus status, Instant createdAt, Instant updatedAt) {
    WarehouseReceipt changeStatus(ReceiptStatus nextStatus) {
        return new WarehouseReceipt(receiptId, skuId, warehouseCode, batchNo, quantity, expiresOn, nextStatus,
                createdAt, Instant.now());
    }
}

record InventoryTransfer(long transferId, long skuId, String fromWarehouse, String toWarehouse, int quantity,
        TransferStatus status, Instant createdAt, Instant updatedAt) {
    InventoryTransfer changeStatus(TransferStatus nextStatus) {
        return new InventoryTransfer(transferId, skuId, fromWarehouse, toWarehouse, quantity, nextStatus, createdAt,
                Instant.now());
    }
}

record LogisticsWaybill(long waybillId, long orderId, String carrierCode, String routeCode, int slaHours,
        WaybillStatus status, String exceptionReason, Instant deliveredAt, Instant createdAt, Instant updatedAt) {
    LogisticsWaybill changeStatus(WaybillStatus nextStatus, String reason, Instant deliveryTime) {
        return new LogisticsWaybill(waybillId, orderId, carrierCode, routeCode, slaHours, nextStatus, reason,
                deliveryTime, createdAt, Instant.now());
    }
}

record SupplyChainSummary(int receipts, int transfers, int waybills, int openExceptions) {
}

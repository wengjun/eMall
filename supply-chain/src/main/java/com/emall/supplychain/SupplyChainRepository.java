package com.emall.supplychain;

import java.util.List;
import java.util.Optional;

interface SupplyChainRepository {
    WarehouseReceipt saveReceipt(WarehouseReceipt receipt);

    Optional<WarehouseReceipt> findReceipt(long receiptId);

    List<WarehouseReceipt> findReceipts(String warehouseCode);

    InventoryTransfer saveTransfer(InventoryTransfer transfer);

    Optional<InventoryTransfer> findTransfer(long transferId);

    List<InventoryTransfer> findTransfers(String warehouseCode);

    LogisticsWaybill saveWaybill(LogisticsWaybill waybill);

    Optional<LogisticsWaybill> findWaybill(long waybillId);

    List<LogisticsWaybill> findWaybills();
}

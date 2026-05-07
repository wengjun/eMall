package com.emall.supplychain;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemorySupplyChainRepository implements SupplyChainRepository {
    private final ConcurrentMap<Long, WarehouseReceipt> receipts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, InventoryTransfer> transfers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, LogisticsWaybill> waybills = new ConcurrentHashMap<>();

    @Override
    public WarehouseReceipt saveReceipt(WarehouseReceipt receipt) {
        receipts.put(receipt.receiptId(), receipt);
        return receipt;
    }

    @Override
    public Optional<WarehouseReceipt> findReceipt(long receiptId) {
        return Optional.ofNullable(receipts.get(receiptId));
    }

    @Override
    public List<WarehouseReceipt> findReceipts(String warehouseCode) {
        return receipts.values().stream().filter(receipt -> receipt.warehouseCode().equals(warehouseCode)).toList();
    }

    @Override
    public InventoryTransfer saveTransfer(InventoryTransfer transfer) {
        transfers.put(transfer.transferId(), transfer);
        return transfer;
    }

    @Override
    public Optional<InventoryTransfer> findTransfer(long transferId) {
        return Optional.ofNullable(transfers.get(transferId));
    }

    @Override
    public List<InventoryTransfer> findTransfers(String warehouseCode) {
        return transfers.values().stream().filter(transfer -> transfer.fromWarehouse().equals(warehouseCode)
                || transfer.toWarehouse().equals(warehouseCode)).toList();
    }

    @Override
    public LogisticsWaybill saveWaybill(LogisticsWaybill waybill) {
        waybills.put(waybill.waybillId(), waybill);
        return waybill;
    }

    @Override
    public Optional<LogisticsWaybill> findWaybill(long waybillId) {
        return Optional.ofNullable(waybills.get(waybillId));
    }

    @Override
    public List<LogisticsWaybill> findWaybills() {
        return List.copyOf(waybills.values());
    }
}

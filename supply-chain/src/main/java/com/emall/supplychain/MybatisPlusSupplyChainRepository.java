package com.emall.supplychain;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusSupplyChainRepository implements SupplyChainRepository {
    private final SupplyChainMapper supplyChainMapper;

    MybatisPlusSupplyChainRepository(SupplyChainMapper supplyChainMapper) {
        this.supplyChainMapper = supplyChainMapper;
    }

    @Override
    public WarehouseReceipt saveReceipt(WarehouseReceipt receipt) {
        supplyChainMapper.saveReceipt(receipt);
        return receipt;
    }

    @Override
    public Optional<WarehouseReceipt> findReceipt(long receiptId) {
        return Optional.ofNullable(supplyChainMapper.findReceipt(receiptId));
    }

    @Override
    public List<WarehouseReceipt> findReceipts(String warehouseCode) {
        return supplyChainMapper.findReceipts(warehouseCode);
    }

    @Override
    public InventoryTransfer saveTransfer(InventoryTransfer transfer) {
        supplyChainMapper.saveTransfer(transfer);
        return transfer;
    }

    @Override
    public Optional<InventoryTransfer> findTransfer(long transferId) {
        return Optional.ofNullable(supplyChainMapper.findTransfer(transferId));
    }

    @Override
    public List<InventoryTransfer> findTransfers(String warehouseCode) {
        return supplyChainMapper.findTransfers(warehouseCode);
    }

    @Override
    public LogisticsWaybill saveWaybill(LogisticsWaybill waybill) {
        supplyChainMapper.saveWaybill(waybill);
        return waybill;
    }

    @Override
    public Optional<LogisticsWaybill> findWaybill(long waybillId) {
        return Optional.ofNullable(supplyChainMapper.findWaybill(waybillId));
    }

    @Override
    public List<LogisticsWaybill> findWaybills() {
        return supplyChainMapper.findWaybills();
    }
}

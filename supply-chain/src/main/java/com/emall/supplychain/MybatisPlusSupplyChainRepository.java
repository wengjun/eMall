package com.emall.supplychain;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusSupplyChainRepository implements SupplyChainRepository {
    private final SupplyChainMapper supplyChainMapper;
    private final WarehouseReceiptMapper receiptMapper;
    private final InventoryTransferMapper transferMapper;
    private final LogisticsWaybillMapper waybillMapper;

    MybatisPlusSupplyChainRepository(SupplyChainMapper supplyChainMapper, WarehouseReceiptMapper receiptMapper,
            InventoryTransferMapper transferMapper, LogisticsWaybillMapper waybillMapper) {
        this.supplyChainMapper = supplyChainMapper;
        this.receiptMapper = receiptMapper;
        this.transferMapper = transferMapper;
        this.waybillMapper = waybillMapper;
    }

    @Override
    public WarehouseReceipt saveReceipt(WarehouseReceipt receipt) {
        supplyChainMapper.saveReceipt(receipt);
        return receipt;
    }

    @Override
    public Optional<WarehouseReceipt> findReceipt(long receiptId) {
        return Optional.ofNullable(receiptMapper.selectById(receiptId));
    }

    @Override
    public List<WarehouseReceipt> findReceipts(String warehouseCode) {
        return receiptMapper.selectList(new QueryWrapper<WarehouseReceipt>().eq("warehouse_code", warehouseCode));
    }

    @Override
    public InventoryTransfer saveTransfer(InventoryTransfer transfer) {
        supplyChainMapper.saveTransfer(transfer);
        return transfer;
    }

    @Override
    public Optional<InventoryTransfer> findTransfer(long transferId) {
        return Optional.ofNullable(transferMapper.selectById(transferId));
    }

    @Override
    public List<InventoryTransfer> findTransfers(String warehouseCode) {
        return transferMapper.selectList(
                new QueryWrapper<InventoryTransfer>().eq("from_warehouse", warehouseCode).or()
                        .eq("to_warehouse", warehouseCode));
    }

    @Override
    public LogisticsWaybill saveWaybill(LogisticsWaybill waybill) {
        supplyChainMapper.saveWaybill(waybill);
        return waybill;
    }

    @Override
    public Optional<LogisticsWaybill> findWaybill(long waybillId) {
        return Optional.ofNullable(waybillMapper.selectById(waybillId));
    }

    @Override
    public List<LogisticsWaybill> findWaybills() {
        return waybillMapper.selectList(null);
    }
}

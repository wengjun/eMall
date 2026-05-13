package com.emall.supplychain;

import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.localDateValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;
import static com.emall.common.persistence.RowMaps.value;

import java.util.List;
import java.util.Map;
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
        return Optional.ofNullable(supplyChainMapper.findReceipt(receiptId)).map(this::mapReceipt);
    }

    @Override
    public List<WarehouseReceipt> findReceipts(String warehouseCode) {
        return supplyChainMapper.findReceipts(warehouseCode).stream().map(this::mapReceipt).toList();
    }

    @Override
    public InventoryTransfer saveTransfer(InventoryTransfer transfer) {
        supplyChainMapper.saveTransfer(transfer);
        return transfer;
    }

    @Override
    public Optional<InventoryTransfer> findTransfer(long transferId) {
        return Optional.ofNullable(supplyChainMapper.findTransfer(transferId)).map(this::mapTransfer);
    }

    @Override
    public List<InventoryTransfer> findTransfers(String warehouseCode) {
        return supplyChainMapper.findTransfers(warehouseCode).stream().map(this::mapTransfer).toList();
    }

    @Override
    public LogisticsWaybill saveWaybill(LogisticsWaybill waybill) {
        supplyChainMapper.saveWaybill(waybill);
        return waybill;
    }

    @Override
    public Optional<LogisticsWaybill> findWaybill(long waybillId) {
        return Optional.ofNullable(supplyChainMapper.findWaybill(waybillId)).map(this::mapWaybill);
    }

    @Override
    public List<LogisticsWaybill> findWaybills() {
        return supplyChainMapper.findWaybills().stream().map(this::mapWaybill).toList();
    }

    private WarehouseReceipt mapReceipt(Map<String, Object> row) {
        return new WarehouseReceipt(longValue(row, "receipt_id"), longValue(row, "sku_id"),
                stringValue(row, "warehouse_code"), stringValue(row, "batch_no"), intValue(row, "quantity"),
                localDateValue(row, "expires_on"), ReceiptStatus.valueOf(stringValue(row, "status")),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private InventoryTransfer mapTransfer(Map<String, Object> row) {
        return new InventoryTransfer(longValue(row, "transfer_id"), longValue(row, "sku_id"),
                stringValue(row, "from_warehouse"), stringValue(row, "to_warehouse"), intValue(row, "quantity"),
                TransferStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private LogisticsWaybill mapWaybill(Map<String, Object> row) {
        return new LogisticsWaybill(longValue(row, "waybill_id"), longValue(row, "order_id"),
                stringValue(row, "carrier_code"), stringValue(row, "route_code"), intValue(row, "sla_hours"),
                WaybillStatus.valueOf(stringValue(row, "status")), stringValue(row, "exception_reason"),
                value(row, "delivered_at") == null ? null : instantValue(row, "delivered_at"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }
}

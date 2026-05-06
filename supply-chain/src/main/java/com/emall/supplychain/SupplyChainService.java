package com.emall.supplychain;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SupplyChainService {
    private final SupplyChainRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    SupplyChainService(SupplyChainRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    WarehouseReceipt receiveStock(long skuId, String warehouseCode, String batchNo, int quantity,
                                  LocalDate expiresOn) {
        requirePositive(quantity, "receipt quantity must be positive");
        Instant now = Instant.now();
        WarehouseReceipt receipt = new WarehouseReceipt(idGenerator.nextId(), skuId, normalize(warehouseCode),
                normalize(batchNo), quantity, expiresOn, ReceiptStatus.RECEIVED, now, now);
        return repository.saveReceipt(receipt);
    }

    @Transactional
    WarehouseReceipt shelve(long receiptId) {
        WarehouseReceipt receipt = requireReceipt(receiptId);
        if (receipt.status() != ReceiptStatus.RECEIVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "receipt must be received before shelving");
        }
        return repository.saveReceipt(receipt.changeStatus(ReceiptStatus.SHELVED));
    }

    @Transactional
    InventoryTransfer createTransfer(long skuId, String fromWarehouse, String toWarehouse, int quantity) {
        requirePositive(quantity, "transfer quantity must be positive");
        Instant now = Instant.now();
        InventoryTransfer transfer = new InventoryTransfer(idGenerator.nextId(), skuId, normalize(fromWarehouse),
                normalize(toWarehouse), quantity, TransferStatus.CREATED, now, now);
        return repository.saveTransfer(transfer);
    }

    @Transactional
    InventoryTransfer changeTransferStatus(long transferId, TransferStatus status) {
        InventoryTransfer transfer = repository.findTransfer(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "inventory transfer not found"));
        return repository.saveTransfer(transfer.changeStatus(status));
    }

    @Transactional
    LogisticsWaybill createWaybill(long orderId, String carrierCode, String routeCode, int slaHours) {
        requirePositive(slaHours, "sla hours must be positive");
        Instant now = Instant.now();
        LogisticsWaybill waybill = new LogisticsWaybill(idGenerator.nextId(), orderId, normalize(carrierCode),
                normalize(routeCode), slaHours, WaybillStatus.IN_TRANSIT, "", null, now, now);
        return repository.saveWaybill(waybill);
    }

    @Transactional
    LogisticsWaybill reportDeliveryException(long waybillId, String reason) {
        LogisticsWaybill waybill = requireWaybill(waybillId);
        return repository.saveWaybill(waybill.changeStatus(WaybillStatus.EXCEPTION, reason, waybill.deliveredAt()));
    }

    @Transactional
    LogisticsWaybill confirmDelivery(long waybillId) {
        LogisticsWaybill waybill = requireWaybill(waybillId);
        return repository.saveWaybill(waybill.changeStatus(WaybillStatus.DELIVERED, "", Instant.now()));
    }

    SupplyChainSummary summary(String warehouseCode) {
        int receipts = repository.findReceipts(normalize(warehouseCode)).size();
        int transfers = repository.findTransfers(normalize(warehouseCode)).size();
        int waybills = repository.findWaybills().size();
        int exceptions = (int) repository.findWaybills().stream()
                .filter(waybill -> waybill.status() == WaybillStatus.EXCEPTION)
                .count();
        return new SupplyChainSummary(receipts, transfers, waybills, exceptions);
    }

    private WarehouseReceipt requireReceipt(long receiptId) {
        return repository.findReceipt(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "warehouse receipt not found"));
    }

    private LogisticsWaybill requireWaybill(long waybillId) {
        return repository.findWaybill(waybillId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "logistics waybill not found"));
    }

    private void requirePositive(int value, String message) {
        if (value <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "supply chain value must not be blank");
        }
        return normalized;
    }
}

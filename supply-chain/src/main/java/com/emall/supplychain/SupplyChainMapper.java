package com.emall.supplychain;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface SupplyChainMapper {
    @Insert("""
            INSERT INTO warehouse_receipt
                (receipt_id, sku_id, warehouse_code, batch_no, quantity, expires_on, status, created_at,
                updated_at)
            VALUES (#{receipt.receiptId}, #{receipt.skuId}, #{receipt.warehouseCode}, #{receipt.batchNo},
                #{receipt.quantity}, #{receipt.expiresOn}, #{receipt.status}, #{receipt.createdAt},
                #{receipt.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveReceipt(@Param("receipt") WarehouseReceipt receipt);

    @Insert("""
            INSERT INTO inventory_transfer
                (transfer_id, sku_id, from_warehouse, to_warehouse, quantity, status, created_at, updated_at)
            VALUES (#{transfer.transferId}, #{transfer.skuId}, #{transfer.fromWarehouse},
                #{transfer.toWarehouse}, #{transfer.quantity}, #{transfer.status}, #{transfer.createdAt},
                #{transfer.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveTransfer(@Param("transfer") InventoryTransfer transfer);

    @Insert("""
            INSERT INTO logistics_waybill
                (waybill_id, order_id, carrier_code, route_code, sla_hours, status, exception_reason,
                delivered_at, created_at, updated_at)
            VALUES (#{waybill.waybillId}, #{waybill.orderId}, #{waybill.carrierCode}, #{waybill.routeCode},
                #{waybill.slaHours}, #{waybill.status}, #{waybill.exceptionReason}, #{waybill.deliveredAt},
                #{waybill.createdAt}, #{waybill.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), exception_reason = VALUES(exception_reason),
                delivered_at = VALUES(delivered_at), updated_at = VALUES(updated_at)
            """)
    int saveWaybill(@Param("waybill") LogisticsWaybill waybill);
}

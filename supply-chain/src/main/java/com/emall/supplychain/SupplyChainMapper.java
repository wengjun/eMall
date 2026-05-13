package com.emall.supplychain;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    @Select("SELECT * FROM warehouse_receipt WHERE receipt_id = #{receiptId}")
    Map<String, Object> findReceipt(@Param("receiptId") long receiptId);

    @Select("SELECT * FROM warehouse_receipt WHERE warehouse_code = #{warehouseCode}")
    List<Map<String, Object>> findReceipts(@Param("warehouseCode") String warehouseCode);

    @Insert("""
            INSERT INTO inventory_transfer
                (transfer_id, sku_id, from_warehouse, to_warehouse, quantity, status, created_at, updated_at)
            VALUES (#{transfer.transferId}, #{transfer.skuId}, #{transfer.fromWarehouse},
                #{transfer.toWarehouse}, #{transfer.quantity}, #{transfer.status}, #{transfer.createdAt},
                #{transfer.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveTransfer(@Param("transfer") InventoryTransfer transfer);

    @Select("SELECT * FROM inventory_transfer WHERE transfer_id = #{transferId}")
    Map<String, Object> findTransfer(@Param("transferId") long transferId);

    @Select("""
            SELECT * FROM inventory_transfer
            WHERE from_warehouse = #{warehouseCode} OR to_warehouse = #{warehouseCode}
            """)
    List<Map<String, Object>> findTransfers(@Param("warehouseCode") String warehouseCode);

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

    @Select("SELECT * FROM logistics_waybill WHERE waybill_id = #{waybillId}")
    Map<String, Object> findWaybill(@Param("waybillId") long waybillId);

    @Select("SELECT * FROM logistics_waybill")
    List<Map<String, Object>> findWaybills();
}

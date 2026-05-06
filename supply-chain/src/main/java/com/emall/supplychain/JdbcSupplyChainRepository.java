package com.emall.supplychain;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcSupplyChainRepository implements SupplyChainRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcSupplyChainRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public WarehouseReceipt saveReceipt(WarehouseReceipt receipt) {
        jdbcTemplate.update("""
                INSERT INTO warehouse_receipt
                    (receipt_id, sku_id, warehouse_code, batch_no, quantity, expires_on, status, created_at,
                    updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, receipt.receiptId(), receipt.skuId(), receipt.warehouseCode(), receipt.batchNo(),
                receipt.quantity(), Date.valueOf(receipt.expiresOn()), receipt.status().name(),
                Timestamp.from(receipt.createdAt()), Timestamp.from(receipt.updatedAt()));
        return receipt;
    }

    @Override
    public Optional<WarehouseReceipt> findReceipt(long receiptId) {
        return jdbcTemplate.query("SELECT * FROM warehouse_receipt WHERE receipt_id = ?", this::mapReceipt,
                receiptId).stream().findFirst();
    }

    @Override
    public List<WarehouseReceipt> findReceipts(String warehouseCode) {
        return jdbcTemplate.query("SELECT * FROM warehouse_receipt WHERE warehouse_code = ?", this::mapReceipt,
                warehouseCode);
    }

    @Override
    public InventoryTransfer saveTransfer(InventoryTransfer transfer) {
        jdbcTemplate.update("""
                INSERT INTO inventory_transfer
                    (transfer_id, sku_id, from_warehouse, to_warehouse, quantity, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, transfer.transferId(), transfer.skuId(), transfer.fromWarehouse(), transfer.toWarehouse(),
                transfer.quantity(), transfer.status().name(), Timestamp.from(transfer.createdAt()),
                Timestamp.from(transfer.updatedAt()));
        return transfer;
    }

    @Override
    public Optional<InventoryTransfer> findTransfer(long transferId) {
        return jdbcTemplate.query("SELECT * FROM inventory_transfer WHERE transfer_id = ?", this::mapTransfer,
                transferId).stream().findFirst();
    }

    @Override
    public List<InventoryTransfer> findTransfers(String warehouseCode) {
        return jdbcTemplate.query("""
                SELECT * FROM inventory_transfer
                WHERE from_warehouse = ? OR to_warehouse = ?
                """, this::mapTransfer, warehouseCode, warehouseCode);
    }

    @Override
    public LogisticsWaybill saveWaybill(LogisticsWaybill waybill) {
        jdbcTemplate.update("""
                INSERT INTO logistics_waybill
                    (waybill_id, order_id, carrier_code, route_code, sla_hours, status, exception_reason,
                    delivered_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), exception_reason = VALUES(exception_reason),
                    delivered_at = VALUES(delivered_at), updated_at = VALUES(updated_at)
                """, waybill.waybillId(), waybill.orderId(), waybill.carrierCode(), waybill.routeCode(),
                waybill.slaHours(), waybill.status().name(), waybill.exceptionReason(),
                waybill.deliveredAt() == null ? null : Timestamp.from(waybill.deliveredAt()),
                Timestamp.from(waybill.createdAt()), Timestamp.from(waybill.updatedAt()));
        return waybill;
    }

    @Override
    public Optional<LogisticsWaybill> findWaybill(long waybillId) {
        return jdbcTemplate.query("SELECT * FROM logistics_waybill WHERE waybill_id = ?", this::mapWaybill,
                waybillId).stream().findFirst();
    }

    @Override
    public List<LogisticsWaybill> findWaybills() {
        return jdbcTemplate.query("SELECT * FROM logistics_waybill", this::mapWaybill);
    }

    private WarehouseReceipt mapReceipt(ResultSet rs, int rowNum) throws SQLException {
        return new WarehouseReceipt(rs.getLong("receipt_id"), rs.getLong("sku_id"),
                rs.getString("warehouse_code"), rs.getString("batch_no"), rs.getInt("quantity"),
                rs.getDate("expires_on").toLocalDate(), ReceiptStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private InventoryTransfer mapTransfer(ResultSet rs, int rowNum) throws SQLException {
        return new InventoryTransfer(rs.getLong("transfer_id"), rs.getLong("sku_id"),
                rs.getString("from_warehouse"), rs.getString("to_warehouse"), rs.getInt("quantity"),
                TransferStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private LogisticsWaybill mapWaybill(ResultSet rs, int rowNum) throws SQLException {
        Timestamp deliveredAt = rs.getTimestamp("delivered_at");
        return new LogisticsWaybill(rs.getLong("waybill_id"), rs.getLong("order_id"),
                rs.getString("carrier_code"), rs.getString("route_code"), rs.getInt("sla_hours"),
                WaybillStatus.valueOf(rs.getString("status")), rs.getString("exception_reason"),
                deliveredAt == null ? null : deliveredAt.toInstant(), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}

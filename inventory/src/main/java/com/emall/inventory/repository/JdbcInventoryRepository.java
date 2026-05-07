package com.emall.inventory.repository;

import com.emall.inventory.domain.InventoryBucket;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.domain.ReservationStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class JdbcInventoryRepository implements InventoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcInventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public InventoryItem saveItem(InventoryItem item) {
        jdbcTemplate.update("""
                INSERT INTO inventory_item (sku_id, total, reserved, sold, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE total = VALUES(total), reserved = VALUES(reserved), sold = VALUES(sold),
                    updated_at = VALUES(updated_at)
                """, item.skuId(), item.total(), item.reserved(), item.sold(), Timestamp.from(item.updatedAt()));
        return item;
    }

    @Override
    public Optional<InventoryItem> findItem(long skuId) {
        return jdbcTemplate.query("SELECT * FROM inventory_item WHERE sku_id = ?", this::mapItem, skuId).stream()
                .findFirst();
    }

    @Override
    public InventoryBucket saveBucket(InventoryBucket bucket) {
        jdbcTemplate.update("""
                INSERT INTO inventory_bucket (sku_id, bucket_no, total, reserved, sold, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE total = VALUES(total), reserved = VALUES(reserved), sold = VALUES(sold),
                    updated_at = VALUES(updated_at)
                """, bucket.skuId(), bucket.bucketNo(), bucket.total(), bucket.reserved(), bucket.sold(),
                Timestamp.from(bucket.updatedAt()));
        return bucket;
    }

    @Override
    public List<InventoryBucket> findBuckets(long skuId) {
        return jdbcTemplate.query("SELECT * FROM inventory_bucket WHERE sku_id = ? ORDER BY bucket_no ASC",
                this::mapBucket, skuId);
    }

    @Override
    public Optional<InventoryBucket> findBucket(long skuId, int bucketNo) {
        return jdbcTemplate.query("SELECT * FROM inventory_bucket WHERE sku_id = ? AND bucket_no = ?", this::mapBucket,
                skuId, bucketNo).stream().findFirst();
    }

    @Override
    public Optional<InventoryBucket> findReservableBucket(long skuId, int quantity) {
        return jdbcTemplate.query("""
                SELECT * FROM inventory_bucket
                WHERE sku_id = ? AND total - reserved - sold >= ?
                ORDER BY reserved ASC, bucket_no ASC
                LIMIT 1
                """, this::mapBucket, skuId, quantity).stream().findFirst();
    }

    @Override
    public InventoryReservation saveReservation(InventoryReservation reservation) {
        jdbcTemplate.update("""
                INSERT INTO inventory_reservation
                    (request_id, sku_id, quantity, bucket_no, status, reason, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), reason = VALUES(reason),
                    updated_at = VALUES(updated_at)
                """, reservation.requestId(), reservation.skuId(), reservation.quantity(), reservation.bucketNo(),
                reservation.status().name(), reservation.reason(), Timestamp.from(reservation.expiresAt()),
                Timestamp.from(reservation.createdAt()), Timestamp.from(reservation.updatedAt()));
        return reservation;
    }

    @Override
    public Optional<InventoryReservation> findReservation(String requestId) {
        return jdbcTemplate
                .query("SELECT * FROM inventory_reservation WHERE request_id = ?", this::mapReservation, requestId)
                .stream().findFirst();
    }

    @Override
    public List<InventoryReservation> findExpiredReservations(Instant now, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM inventory_reservation
                WHERE status = 'RESERVED' AND expires_at <= ?
                ORDER BY expires_at ASC
                LIMIT ?
                """, this::mapReservation, Timestamp.from(now), limit);
    }

    private InventoryItem mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new InventoryItem(rs.getLong("sku_id"), rs.getInt("total"), rs.getInt("reserved"), rs.getInt("sold"),
                rs.getTimestamp("updated_at").toInstant());
    }

    private InventoryBucket mapBucket(ResultSet rs, int rowNum) throws SQLException {
        return new InventoryBucket(rs.getLong("sku_id"), rs.getInt("bucket_no"), rs.getInt("total"),
                rs.getInt("reserved"), rs.getInt("sold"), rs.getTimestamp("updated_at").toInstant());
    }

    private InventoryReservation mapReservation(ResultSet rs, int rowNum) throws SQLException {
        int bucketNo = rs.getInt("bucket_no");
        return new InventoryReservation(rs.getString("request_id"), rs.getLong("sku_id"), rs.getInt("quantity"),
                rs.wasNull() ? null : bucketNo, ReservationStatus.valueOf(rs.getString("status")),
                rs.getString("reason"), rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}

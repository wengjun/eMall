package com.emall.order.repository;

import com.emall.order.domain.Order;
import com.emall.order.domain.OrderStatus;
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
public class JdbcOrderRepository implements OrderRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Order save(Order order) {
        jdbcTemplate.update("""
                INSERT INTO order_record
                    (order_id, request_id, user_id, sku_id, quantity, unit_price, subtotal_amount, discount_amount,
                    payable_amount, currency, price_version, coupon_id, inventory_reservation_id, status,
                    failure_reason, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE unit_price = VALUES(unit_price),
                    subtotal_amount = VALUES(subtotal_amount), discount_amount = VALUES(discount_amount),
                    payable_amount = VALUES(payable_amount), currency = VALUES(currency),
                    price_version = VALUES(price_version), coupon_id = VALUES(coupon_id),
                    inventory_reservation_id = VALUES(inventory_reservation_id), status = VALUES(status),
                    failure_reason = VALUES(failure_reason), updated_at = VALUES(updated_at)
                """, order.orderId(), order.requestId(), order.userId(), order.skuId(), order.quantity(),
                order.unitPrice(), order.subtotalAmount(), order.discountAmount(), order.payableAmount(),
                order.currency(), order.priceVersion(), order.couponId(), order.inventoryReservationId(),
                order.status().name(), order.failureReason(), Timestamp.from(order.createdAt()),
                Timestamp.from(order.updatedAt()));
        return order;
    }

    @Override
    public Optional<Order> findById(long orderId) {
        return jdbcTemplate.query("SELECT * FROM order_record WHERE order_id = ?", this::map, orderId).stream()
                .findFirst();
    }

    @Override
    public Optional<Order> findByRequestId(String requestId) {
        return jdbcTemplate.query("SELECT * FROM order_record WHERE request_id = ?", this::map, requestId).stream()
                .findFirst();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM order_record
                WHERE status = ?
                ORDER BY updated_at ASC
                LIMIT ?
                """, this::map, status.name(), limit);
    }

    private Order map(ResultSet rs, int rowNum) throws SQLException {
        return new Order(rs.getLong("order_id"), rs.getString("request_id"), rs.getLong("user_id"),
                rs.getLong("sku_id"), rs.getInt("quantity"), rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("subtotal_amount"), rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("payable_amount"), rs.getString("currency"), rs.getLong("price_version"),
                rs.getString("coupon_id"), rs.getString("inventory_reservation_id"),
                OrderStatus.valueOf(rs.getString("status")), rs.getString("failure_reason"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}

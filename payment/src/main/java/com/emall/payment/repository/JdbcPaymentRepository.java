package com.emall.payment.repository;

import com.emall.payment.domain.PaymentOrder;
import com.emall.payment.domain.PaymentStatus;
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
public class JdbcPaymentRepository implements PaymentRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcPaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PaymentOrder save(PaymentOrder payment) {
        jdbcTemplate.update("""
                INSERT INTO payment_order
                    (payment_id, request_id, order_id, user_id, amount, channel, channel_trade_no, status,
                    order_confirmed, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE channel_trade_no = VALUES(channel_trade_no), status = VALUES(status),
                    order_confirmed = VALUES(order_confirmed), updated_at = VALUES(updated_at)
                """,
                payment.paymentId(), payment.requestId(), payment.orderId(), payment.userId(), payment.amount(),
                payment.channel(), payment.channelTradeNo(), payment.status().name(), payment.orderConfirmed(),
                Timestamp.from(payment.createdAt()), Timestamp.from(payment.updatedAt()));
        return payment;
    }

    @Override
    public Optional<PaymentOrder> findById(long paymentId) {
        return jdbcTemplate.query("SELECT * FROM payment_order WHERE payment_id = ?", this::map, paymentId)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<PaymentOrder> findByRequestId(String requestId) {
        return jdbcTemplate.query("SELECT * FROM payment_order WHERE request_id = ?", this::map, requestId)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<PaymentOrder> findByChannelTradeNo(String channelTradeNo) {
        return jdbcTemplate.query("SELECT * FROM payment_order WHERE channel_trade_no = ?", this::map, channelTradeNo)
                .stream()
                .findFirst();
    }

    @Override
    public List<PaymentOrder> findUnconfirmedByStatus(PaymentStatus status, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM payment_order
                WHERE status = ? AND order_confirmed = false
                ORDER BY updated_at ASC
                LIMIT ?
                """, this::map, status.name(), limit);
    }

    private PaymentOrder map(ResultSet rs, int rowNum) throws SQLException {
        return new PaymentOrder(
                rs.getLong("payment_id"),
                rs.getString("request_id"),
                rs.getLong("order_id"),
                rs.getLong("user_id"),
                rs.getBigDecimal("amount"),
                rs.getString("channel"),
                rs.getString("channel_trade_no"),
                PaymentStatus.valueOf(rs.getString("status")),
                rs.getBoolean("order_confirmed"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}

package com.emall.aftersales.repository;

import com.emall.aftersales.domain.AfterSalesRequest;
import com.emall.aftersales.domain.AfterSalesStatus;
import com.emall.aftersales.domain.AfterSalesType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class JdbcAfterSalesRepository implements AfterSalesRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAfterSalesRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AfterSalesRequest save(AfterSalesRequest request) {
        jdbcTemplate.update("""
                INSERT INTO after_sales_request
                    (request_id, order_id, user_id, sku_id, quantity, refund_amount, type, status, reason,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), reason = VALUES(reason),
                    updated_at = VALUES(updated_at)
                """, request.requestId(), request.orderId(), request.userId(), request.skuId(), request.quantity(),
                request.refundAmount(), request.type().name(), request.status().name(), request.reason(),
                Timestamp.from(request.createdAt()), Timestamp.from(request.updatedAt()));
        return request;
    }

    @Override
    public Optional<AfterSalesRequest> findById(long requestId) {
        return jdbcTemplate.query("SELECT * FROM after_sales_request WHERE request_id = ?", this::map, requestId)
                .stream().findFirst();
    }

    private AfterSalesRequest map(ResultSet rs, int rowNum) throws SQLException {
        return new AfterSalesRequest(rs.getLong("request_id"), rs.getLong("order_id"), rs.getLong("user_id"),
                rs.getLong("sku_id"), rs.getInt("quantity"), rs.getBigDecimal("refund_amount"),
                AfterSalesType.valueOf(rs.getString("type")), AfterSalesStatus.valueOf(rs.getString("status")),
                rs.getString("reason"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}

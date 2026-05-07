package com.emall.marketing.repository;

import com.emall.marketing.domain.Coupon;
import com.emall.marketing.domain.CouponStatus;
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
public class JdbcCouponRepository implements CouponRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCouponRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Coupon save(Coupon coupon) {
        jdbcTemplate.update("""
                INSERT INTO coupon
                    (coupon_id, user_id, threshold_amount, discount_amount, status, expires_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), expires_at = VALUES(expires_at),
                    updated_at = VALUES(updated_at)
                """, coupon.couponId(), coupon.userId(), coupon.thresholdAmount(), coupon.discountAmount(),
                coupon.status().name(), Timestamp.from(coupon.expiresAt()), Timestamp.from(coupon.updatedAt()));
        return coupon;
    }

    @Override
    public Optional<Coupon> findById(String couponId) {
        return jdbcTemplate.query("SELECT * FROM coupon WHERE coupon_id = ?", this::map, couponId).stream().findFirst();
    }

    @Override
    public List<Coupon> findByUserId(long userId) {
        return jdbcTemplate.query("SELECT * FROM coupon WHERE user_id = ? ORDER BY updated_at DESC", this::map, userId);
    }

    private Coupon map(ResultSet rs, int rowNum) throws SQLException {
        return new Coupon(rs.getString("coupon_id"), rs.getLong("user_id"), rs.getBigDecimal("threshold_amount"),
                rs.getBigDecimal("discount_amount"), CouponStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("expires_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}

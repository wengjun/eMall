package com.emall.review.repository;

import com.emall.review.domain.ProductReview;
import com.emall.review.domain.ReviewStatus;
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
public class JdbcReviewRepository implements ReviewRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcReviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ProductReview save(ProductReview review) {
        jdbcTemplate.update("""
                INSERT INTO product_review
                    (review_id, order_id, sku_id, user_id, rating, content, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE content = VALUES(content), status = VALUES(status),
                    updated_at = VALUES(updated_at)
                """, review.reviewId(), review.orderId(), review.skuId(), review.userId(), review.rating(),
                review.content(), review.status().name(), Timestamp.from(review.createdAt()),
                Timestamp.from(review.updatedAt()));
        return review;
    }

    @Override
    public Optional<ProductReview> findById(long reviewId) {
        return jdbcTemplate.query("SELECT * FROM product_review WHERE review_id = ?", this::map, reviewId).stream()
                .findFirst();
    }

    @Override
    public List<ProductReview> findBySkuId(long skuId, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM product_review
                WHERE sku_id = ? AND status = 'PUBLISHED'
                ORDER BY created_at DESC
                LIMIT ?
                """, this::map, skuId, limit);
    }

    private ProductReview map(ResultSet rs, int rowNum) throws SQLException {
        return new ProductReview(rs.getLong("review_id"), rs.getLong("order_id"), rs.getLong("sku_id"),
                rs.getLong("user_id"), rs.getInt("rating"), rs.getString("content"),
                ReviewStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}

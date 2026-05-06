package com.emall.product.repository;

import com.emall.product.domain.Product;
import com.emall.product.domain.ProductStatus;
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
public class JdbcProductRepository implements ProductRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Product save(Product product) {
        jdbcTemplate.update("""
                INSERT INTO product (sku_id, spu_id, title, category, price, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE title = VALUES(title), category = VALUES(category), price = VALUES(price),
                    status = VALUES(status), updated_at = VALUES(updated_at)
                """,
                product.skuId(), product.spuId(), product.title(), product.category(), product.price(),
                product.status().name(), Timestamp.from(product.createdAt()), Timestamp.from(product.updatedAt()));
        return product;
    }

    @Override
    public Optional<Product> findBySkuId(long skuId) {
        return jdbcTemplate.query("SELECT * FROM product WHERE sku_id = ?", this::map, skuId)
                .stream()
                .findFirst();
    }

    @Override
    public List<Product> search(String keyword, int limit) {
        String pattern = "%" + (keyword == null ? "" : keyword) + "%";
        return jdbcTemplate.query("""
                SELECT * FROM product
                WHERE status = 'ON_SALE' AND (title LIKE ? OR category LIKE ?)
                ORDER BY updated_at DESC
                LIMIT ?
                """, this::map, pattern, pattern, limit);
    }

    private Product map(ResultSet rs, int rowNum) throws SQLException {
        return new Product(
                rs.getLong("sku_id"),
                rs.getLong("spu_id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getBigDecimal("price"),
                ProductStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}

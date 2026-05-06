package com.emall.cart.repository;

import com.emall.cart.domain.CartItem;
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
public class JdbcCartRepository implements CartRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCartRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CartItem save(CartItem item) {
        jdbcTemplate.update("""
                INSERT INTO cart_item (user_id, sku_id, quantity, selected, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE quantity = VALUES(quantity), selected = VALUES(selected),
                    updated_at = VALUES(updated_at)
                """,
                item.userId(), item.skuId(), item.quantity(), item.selected(), Timestamp.from(item.updatedAt()));
        return item;
    }

    @Override
    public Optional<CartItem> find(long userId, long skuId) {
        return jdbcTemplate.query("SELECT * FROM cart_item WHERE user_id = ? AND sku_id = ?", this::map, userId, skuId)
                .stream()
                .findFirst();
    }

    @Override
    public List<CartItem> findByUserId(long userId) {
        return jdbcTemplate.query("SELECT * FROM cart_item WHERE user_id = ? ORDER BY updated_at DESC", this::map,
                userId);
    }

    @Override
    public void delete(long userId, long skuId) {
        jdbcTemplate.update("DELETE FROM cart_item WHERE user_id = ? AND sku_id = ?", userId, skuId);
    }

    @Override
    public void clearSelected(long userId) {
        jdbcTemplate.update("DELETE FROM cart_item WHERE user_id = ? AND selected = true", userId);
    }

    private CartItem map(ResultSet rs, int rowNum) throws SQLException {
        return new CartItem(
                rs.getLong("user_id"),
                rs.getLong("sku_id"),
                rs.getInt("quantity"),
                rs.getBoolean("selected"),
                rs.getTimestamp("updated_at").toInstant());
    }
}

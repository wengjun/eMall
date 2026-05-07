package com.emall.pricing.repository;

import com.emall.pricing.domain.PriceBook;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class JdbcPriceRepository implements PriceRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcPriceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PriceBook save(PriceBook priceBook) {
        jdbcTemplate.update("""
                INSERT INTO price_book (sku_id, list_price, sale_price, currency, version, active, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE list_price = VALUES(list_price), sale_price = VALUES(sale_price),
                    currency = VALUES(currency), version = VALUES(version), active = VALUES(active),
                    updated_at = VALUES(updated_at)
                """, priceBook.skuId(), priceBook.listPrice(), priceBook.salePrice(), priceBook.currency(),
                priceBook.version(), priceBook.active(), Timestamp.from(priceBook.updatedAt()));
        return priceBook;
    }

    @Override
    public Optional<PriceBook> findBySkuId(long skuId) {
        return jdbcTemplate.query("SELECT * FROM price_book WHERE sku_id = ?", this::map, skuId).stream().findFirst();
    }

    private PriceBook map(ResultSet rs, int rowNum) throws SQLException {
        return new PriceBook(rs.getLong("sku_id"), rs.getBigDecimal("list_price"), rs.getBigDecimal("sale_price"),
                rs.getString("currency"), rs.getLong("version"), rs.getBoolean("active"),
                rs.getTimestamp("updated_at").toInstant());
    }
}

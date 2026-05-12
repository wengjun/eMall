package com.emall.search.repository;

import com.emall.search.domain.SearchDocument;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.search.engine", havingValue = "jdbc", matchIfMissing = true)
public class JdbcSearchRepository implements SearchRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SearchDocument save(SearchDocument document) {
        jdbcTemplate.update("""
                INSERT INTO search_document (sku_id, title, category, price, tags, saleable, indexed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE title = VALUES(title), category = VALUES(category), price = VALUES(price),
                    tags = VALUES(tags), saleable = VALUES(saleable), indexed_at = VALUES(indexed_at)
                """, document.skuId(), document.title(), document.category(), document.price(),
                serialize(document.tags()), document.saleable(), Timestamp.from(document.indexedAt()));
        return document;
    }

    @Override
    public Optional<SearchDocument> findBySkuId(long skuId) {
        return jdbcTemplate.query("SELECT * FROM search_document WHERE sku_id = ?", this::map, skuId).stream()
                .findFirst();
    }

    @Override
    public List<SearchDocument> search(String keyword, int limit) {
        String pattern = "%" + (keyword == null ? "" : keyword) + "%";
        return jdbcTemplate.query("""
                SELECT * FROM search_document
                WHERE saleable = true AND (title LIKE ? OR category LIKE ? OR tags LIKE ?)
                ORDER BY indexed_at DESC
                LIMIT ?
                """, this::map, pattern, pattern, pattern, limit);
    }

    @Override
    public void delete(long skuId) {
        jdbcTemplate.update("DELETE FROM search_document WHERE sku_id = ?", skuId);
    }

    private SearchDocument map(ResultSet rs, int rowNum) throws SQLException {
        return new SearchDocument(rs.getLong("sku_id"), rs.getString("title"), rs.getString("category"),
                rs.getBigDecimal("price"), deserialize(rs.getString("tags")), rs.getBoolean("saleable"),
                rs.getTimestamp("indexed_at").toInstant());
    }

    private String serialize(Set<String> tags) {
        return tags == null ? "" : String.join(",", tags);
    }

    private Set<String> deserialize(String tags) {
        if (tags == null || tags.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(tags.split(",")).filter(tag -> !tag.isBlank()).collect(Collectors.toUnmodifiableSet());
    }
}

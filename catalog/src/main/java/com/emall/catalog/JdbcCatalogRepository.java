package com.emall.catalog;

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
class JdbcCatalogRepository implements CatalogRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcCatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CategoryNode saveCategory(CategoryNode category) {
        jdbcTemplate.update("""
                INSERT INTO catalog_category (category_id, parent_id, category_code, name, leaf)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), leaf = VALUES(leaf)
                """, category.categoryId(), category.parentId(), category.categoryCode(), category.name(),
                category.leaf());
        return category;
    }

    @Override
    public Optional<CategoryNode> findCategory(long categoryId) {
        return jdbcTemplate.query("SELECT * FROM catalog_category WHERE category_id = ?", this::mapCategory, categoryId)
                .stream().findFirst();
    }

    @Override
    public AttributeTemplate saveTemplate(AttributeTemplate template) {
        jdbcTemplate.update("""
                INSERT INTO catalog_attribute_template
                    (template_id, category_id, required_attributes, optional_attributes)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE required_attributes = VALUES(required_attributes),
                    optional_attributes = VALUES(optional_attributes)
                """, template.templateId(), template.categoryId(), template.requiredAttributes(),
                template.optionalAttributes());
        return template;
    }

    @Override
    public Optional<AttributeTemplate> findTemplate(long categoryId) {
        return jdbcTemplate
                .query("SELECT * FROM catalog_attribute_template WHERE category_id = ?", this::mapTemplate, categoryId)
                .stream().findFirst();
    }

    @Override
    public BrandAuthorization saveBrandAuthorization(BrandAuthorization authorization) {
        jdbcTemplate.update("""
                INSERT INTO catalog_brand_authorization
                    (authorization_id, merchant_id, brand_code, active, created_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE active = VALUES(active)
                """, authorization.authorizationId(), authorization.merchantId(), authorization.brandCode(),
                authorization.active(), Timestamp.from(authorization.createdAt()));
        return authorization;
    }

    @Override
    public boolean hasBrandAuthorization(long merchantId, String brandCode) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM catalog_brand_authorization
                WHERE merchant_id = ? AND brand_code = ? AND active = TRUE
                """, Integer.class, merchantId, brandCode);
        return count != null && count > 0;
    }

    @Override
    public Spu saveSpu(Spu spu) {
        jdbcTemplate.update("""
                INSERT INTO catalog_spu
                    (spu_id, merchant_id, title, category_id, brand_code, status, quality_score,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status),
                    quality_score = VALUES(quality_score), updated_at = VALUES(updated_at)
                """, spu.spuId(), spu.merchantId(), spu.title(), spu.categoryId(), spu.brandCode(), spu.status().name(),
                spu.qualityScore(), Timestamp.from(spu.createdAt()), Timestamp.from(spu.updatedAt()));
        return spu;
    }

    @Override
    public Optional<Spu> findSpu(long spuId) {
        return jdbcTemplate.query("SELECT * FROM catalog_spu WHERE spu_id = ?", this::mapSpu, spuId).stream()
                .findFirst();
    }

    @Override
    public Sku saveSku(Sku sku) {
        jdbcTemplate.update("""
                INSERT INTO catalog_sku (sku_id, spu_id, sku_code, price, attributes, saleable, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE price = VALUES(price), attributes = VALUES(attributes),
                    saleable = VALUES(saleable), updated_at = VALUES(updated_at)
                """, sku.skuId(), sku.spuId(), sku.skuCode(), sku.price(), sku.attributes(), sku.saleable(),
                Timestamp.from(sku.createdAt()), Timestamp.from(sku.updatedAt()));
        return sku;
    }

    @Override
    public List<Sku> findSkus(long spuId) {
        return jdbcTemplate.query("SELECT * FROM catalog_sku WHERE spu_id = ?", this::mapSku, spuId);
    }

    @Override
    public ListingViolation saveViolation(ListingViolation violation) {
        jdbcTemplate.update("""
                INSERT INTO catalog_listing_violation (violation_id, spu_id, violation_type, reason, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, violation.violationId(), violation.spuId(), violation.violationType(), violation.reason(),
                Timestamp.from(violation.createdAt()));
        return violation;
    }

    @Override
    public List<ListingViolation> findViolations(long spuId) {
        return jdbcTemplate.query("SELECT * FROM catalog_listing_violation WHERE spu_id = ?", this::mapViolation,
                spuId);
    }

    private CategoryNode mapCategory(ResultSet rs, int rowNum) throws SQLException {
        return new CategoryNode(rs.getLong("category_id"), rs.getLong("parent_id"), rs.getString("category_code"),
                rs.getString("name"), rs.getBoolean("leaf"));
    }

    private AttributeTemplate mapTemplate(ResultSet rs, int rowNum) throws SQLException {
        return new AttributeTemplate(rs.getLong("template_id"), rs.getLong("category_id"),
                rs.getString("required_attributes"), rs.getString("optional_attributes"));
    }

    private Spu mapSpu(ResultSet rs, int rowNum) throws SQLException {
        return new Spu(rs.getLong("spu_id"), rs.getLong("merchant_id"), rs.getString("title"),
                rs.getLong("category_id"), rs.getString("brand_code"), ListingStatus.valueOf(rs.getString("status")),
                rs.getInt("quality_score"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private Sku mapSku(ResultSet rs, int rowNum) throws SQLException {
        return new Sku(rs.getLong("sku_id"), rs.getLong("spu_id"), rs.getString("sku_code"), rs.getBigDecimal("price"),
                rs.getString("attributes"), rs.getBoolean("saleable"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private ListingViolation mapViolation(ResultSet rs, int rowNum) throws SQLException {
        return new ListingViolation(rs.getLong("violation_id"), rs.getLong("spu_id"), rs.getString("violation_type"),
                rs.getString("reason"), rs.getTimestamp("created_at").toInstant());
    }
}

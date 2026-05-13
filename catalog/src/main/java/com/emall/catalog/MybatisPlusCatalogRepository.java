package com.emall.catalog;

import static com.emall.common.persistence.RowMaps.booleanValue;
import static com.emall.common.persistence.RowMaps.decimalValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusCatalogRepository implements CatalogRepository {
    private final CatalogMapper catalogMapper;

    MybatisPlusCatalogRepository(CatalogMapper catalogMapper) {
        this.catalogMapper = catalogMapper;
    }

    @Override
    public CategoryNode saveCategory(CategoryNode category) {
        catalogMapper.saveCategory(category);
        return category;
    }

    @Override
    public Optional<CategoryNode> findCategory(long categoryId) {
        return Optional.ofNullable(catalogMapper.findCategory(categoryId)).map(this::mapCategory);
    }

    @Override
    public AttributeTemplate saveTemplate(AttributeTemplate template) {
        catalogMapper.saveTemplate(template);
        return template;
    }

    @Override
    public Optional<AttributeTemplate> findTemplate(long categoryId) {
        return Optional.ofNullable(catalogMapper.findTemplate(categoryId)).map(this::mapTemplate);
    }

    @Override
    public BrandAuthorization saveBrandAuthorization(BrandAuthorization authorization) {
        catalogMapper.saveBrandAuthorization(authorization);
        return authorization;
    }

    @Override
    public boolean hasBrandAuthorization(long merchantId, String brandCode) {
        return catalogMapper.countBrandAuthorization(merchantId, brandCode) > 0;
    }

    @Override
    public Spu saveSpu(Spu spu) {
        catalogMapper.saveSpu(spu);
        return spu;
    }

    @Override
    public Optional<Spu> findSpu(long spuId) {
        return Optional.ofNullable(catalogMapper.findSpu(spuId)).map(this::mapSpu);
    }

    @Override
    public Sku saveSku(Sku sku) {
        catalogMapper.saveSku(sku);
        return sku;
    }

    @Override
    public List<Sku> findSkus(long spuId) {
        return catalogMapper.findSkus(spuId).stream().map(this::mapSku).toList();
    }

    @Override
    public ListingViolation saveViolation(ListingViolation violation) {
        catalogMapper.saveViolation(violation);
        return violation;
    }

    @Override
    public List<ListingViolation> findViolations(long spuId) {
        return catalogMapper.findViolations(spuId).stream().map(this::mapViolation).toList();
    }

    private CategoryNode mapCategory(Map<String, Object> row) {
        return new CategoryNode(longValue(row, "category_id"), longValue(row, "parent_id"),
                stringValue(row, "category_code"), stringValue(row, "name"), booleanValue(row, "leaf"));
    }

    private AttributeTemplate mapTemplate(Map<String, Object> row) {
        return new AttributeTemplate(longValue(row, "template_id"), longValue(row, "category_id"),
                stringValue(row, "required_attributes"), stringValue(row, "optional_attributes"));
    }

    private Spu mapSpu(Map<String, Object> row) {
        return new Spu(longValue(row, "spu_id"), longValue(row, "merchant_id"), stringValue(row, "title"),
                longValue(row, "category_id"), stringValue(row, "brand_code"),
                ListingStatus.valueOf(stringValue(row, "status")), intValue(row, "quality_score"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private Sku mapSku(Map<String, Object> row) {
        return new Sku(longValue(row, "sku_id"), longValue(row, "spu_id"), stringValue(row, "sku_code"),
                decimalValue(row, "price"), stringValue(row, "attributes"), booleanValue(row, "saleable"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }

    private ListingViolation mapViolation(Map<String, Object> row) {
        return new ListingViolation(longValue(row, "violation_id"), longValue(row, "spu_id"),
                stringValue(row, "violation_type"), stringValue(row, "reason"), instantValue(row, "created_at"));
    }
}

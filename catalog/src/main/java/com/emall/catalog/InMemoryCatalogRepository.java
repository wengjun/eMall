package com.emall.catalog;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryCatalogRepository implements CatalogRepository {
    private final ConcurrentMap<Long, CategoryNode> categories = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AttributeTemplate> templates = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, BrandAuthorization> authorizations = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Spu> spus = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Sku> skus = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ListingViolation> violations = new ConcurrentHashMap<>();

    @Override
    public CategoryNode saveCategory(CategoryNode category) {
        categories.put(category.categoryId(), category);
        return category;
    }

    @Override
    public Optional<CategoryNode> findCategory(long categoryId) {
        return Optional.ofNullable(categories.get(categoryId));
    }

    @Override
    public AttributeTemplate saveTemplate(AttributeTemplate template) {
        templates.put(template.categoryId(), template);
        return template;
    }

    @Override
    public Optional<AttributeTemplate> findTemplate(long categoryId) {
        return Optional.ofNullable(templates.get(categoryId));
    }

    @Override
    public BrandAuthorization saveBrandAuthorization(BrandAuthorization authorization) {
        authorizations.put(authorization.authorizationId(), authorization);
        return authorization;
    }

    @Override
    public boolean hasBrandAuthorization(long merchantId, String brandCode) {
        return authorizations.values().stream().anyMatch(
                item -> item.merchantId() == merchantId && item.brandCode().equals(brandCode) && item.active());
    }

    @Override
    public Spu saveSpu(Spu spu) {
        spus.put(spu.spuId(), spu);
        return spu;
    }

    @Override
    public Optional<Spu> findSpu(long spuId) {
        return Optional.ofNullable(spus.get(spuId));
    }

    @Override
    public Sku saveSku(Sku sku) {
        skus.put(sku.skuId(), sku);
        return sku;
    }

    @Override
    public List<Sku> findSkus(long spuId) {
        return skus.values().stream().filter(sku -> sku.spuId() == spuId).toList();
    }

    @Override
    public ListingViolation saveViolation(ListingViolation violation) {
        violations.put(violation.violationId(), violation);
        return violation;
    }

    @Override
    public List<ListingViolation> findViolations(long spuId) {
        return violations.values().stream().filter(violation -> violation.spuId() == spuId).toList();
    }
}

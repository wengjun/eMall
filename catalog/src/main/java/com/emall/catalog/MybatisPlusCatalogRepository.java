package com.emall.catalog;

import java.util.List;
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
        return Optional.ofNullable(catalogMapper.findCategory(categoryId));
    }

    @Override
    public AttributeTemplate saveTemplate(AttributeTemplate template) {
        catalogMapper.saveTemplate(template);
        return template;
    }

    @Override
    public Optional<AttributeTemplate> findTemplate(long categoryId) {
        return Optional.ofNullable(catalogMapper.findTemplate(categoryId));
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
        return Optional.ofNullable(catalogMapper.findSpu(spuId));
    }

    @Override
    public Sku saveSku(Sku sku) {
        catalogMapper.saveSku(sku);
        return sku;
    }

    @Override
    public List<Sku> findSkus(long spuId) {
        return catalogMapper.findSkus(spuId);
    }

    @Override
    public ListingViolation saveViolation(ListingViolation violation) {
        catalogMapper.saveViolation(violation);
        return violation;
    }

    @Override
    public List<ListingViolation> findViolations(long spuId) {
        return catalogMapper.findViolations(spuId);
    }
}

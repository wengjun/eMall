package com.emall.catalog;

import java.util.List;
import java.util.Optional;

interface CatalogRepository {
    CategoryNode saveCategory(CategoryNode category);

    Optional<CategoryNode> findCategory(long categoryId);

    AttributeTemplate saveTemplate(AttributeTemplate template);

    Optional<AttributeTemplate> findTemplate(long categoryId);

    BrandAuthorization saveBrandAuthorization(BrandAuthorization authorization);

    boolean hasBrandAuthorization(long merchantId, String brandCode);

    Spu saveSpu(Spu spu);

    Optional<Spu> findSpu(long spuId);

    Sku saveSku(Sku sku);

    List<Sku> findSkus(long spuId);

    ListingViolation saveViolation(ListingViolation violation);

    List<ListingViolation> findViolations(long spuId);
}

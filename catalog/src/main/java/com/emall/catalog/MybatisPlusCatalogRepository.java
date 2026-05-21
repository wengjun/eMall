package com.emall.catalog;

import java.util.List;
import java.util.Optional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusCatalogRepository implements CatalogRepository {
    private final CatalogMapper catalogMapper;
    private final CategoryNodeMapper categoryMapper;
    private final AttributeTemplateMapper templateMapper;
    private final BrandAuthorizationMapper brandAuthorizationMapper;
    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final ListingViolationMapper violationMapper;

    MybatisPlusCatalogRepository(CatalogMapper catalogMapper, CategoryNodeMapper categoryMapper,
            AttributeTemplateMapper templateMapper, BrandAuthorizationMapper brandAuthorizationMapper,
            SpuMapper spuMapper, SkuMapper skuMapper, ListingViolationMapper violationMapper) {
        this.catalogMapper = catalogMapper;
        this.categoryMapper = categoryMapper;
        this.templateMapper = templateMapper;
        this.brandAuthorizationMapper = brandAuthorizationMapper;
        this.spuMapper = spuMapper;
        this.skuMapper = skuMapper;
        this.violationMapper = violationMapper;
    }

    @Override
    public CategoryNode saveCategory(CategoryNode category) {
        catalogMapper.saveCategory(category);
        return category;
    }

    @Override
    public Optional<CategoryNode> findCategory(long categoryId) {
        return Optional.ofNullable(categoryMapper.selectById(categoryId));
    }

    @Override
    public AttributeTemplate saveTemplate(AttributeTemplate template) {
        catalogMapper.saveTemplate(template);
        return template;
    }

    @Override
    public Optional<AttributeTemplate> findTemplate(long categoryId) {
        return Optional.ofNullable(
                templateMapper.selectOne(new QueryWrapper<AttributeTemplate>().eq("category_id", categoryId)));
    }

    @Override
    public BrandAuthorization saveBrandAuthorization(BrandAuthorization authorization) {
        catalogMapper.saveBrandAuthorization(authorization);
        return authorization;
    }

    @Override
    public boolean hasBrandAuthorization(long merchantId, String brandCode) {
        return brandAuthorizationMapper.selectCount(new QueryWrapper<BrandAuthorization>()
                .eq("merchant_id", merchantId)
                .eq("brand_code", brandCode)
                .eq("active", true)) > 0;
    }

    @Override
    public Spu saveSpu(Spu spu) {
        catalogMapper.saveSpu(spu);
        return spu;
    }

    @Override
    public Optional<Spu> findSpu(long spuId) {
        return Optional.ofNullable(spuMapper.selectById(spuId));
    }

    @Override
    public Sku saveSku(Sku sku) {
        catalogMapper.saveSku(sku);
        return sku;
    }

    @Override
    public List<Sku> findSkus(long spuId) {
        return skuMapper.selectList(new QueryWrapper<Sku>().eq("spu_id", spuId));
    }

    @Override
    public ListingViolation saveViolation(ListingViolation violation) {
        violationMapper.insert(violation);
        return violation;
    }

    @Override
    public List<ListingViolation> findViolations(long spuId) {
        return violationMapper.selectList(new QueryWrapper<ListingViolation>().eq("spu_id", spuId));
    }
}

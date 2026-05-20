package com.emall.product.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.product.domain.Product;
import com.emall.product.domain.ProductStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusProductRepository implements ProductRepository {
    private final ProductMapper productMapper;

    public MybatisPlusProductRepository(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public Product save(Product product) {
        ProductEntity entity = toEntity(product);
        try {
            productMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            productMapper.update(null,
                    new UpdateWrapper<ProductEntity>().set("title", entity.getTitle())
                            .set("category", entity.getCategory()).set("price", entity.getPrice())
                            .set("status", entity.getStatus()).set("updated_at", entity.getUpdatedAt())
                            .eq("sku_id", entity.getSkuId()));
        }
        return product;
    }

    @Override
    public Optional<Product> findBySkuId(long skuId) {
        return Optional.ofNullable(productMapper.selectById(skuId)).map(this::toDomain);
    }

    @Override
    public List<Product> search(String keyword, int limit) {
        String pattern = keyword == null ? "" : keyword;
        return productMapper.selectList(new QueryWrapper<ProductEntity>().eq("status", ProductStatus.ON_SALE.name())
                .and(query -> query.like("title", pattern).or().like("category", pattern)).orderByDesc("updated_at")
                .last("LIMIT " + limit)).stream().map(this::toDomain).toList();
    }

    private ProductEntity toEntity(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.setSkuId(product.skuId());
        entity.setSpuId(product.spuId());
        entity.setTitle(product.title());
        entity.setCategory(product.category());
        entity.setPrice(product.price());
        entity.setStatus(product.status().name());
        entity.setCreatedAt(LocalDateTime.ofInstant(product.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(product.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private Product toDomain(ProductEntity entity) {
        return new Product(entity.getSkuId(), entity.getSpuId(), entity.getTitle(), entity.getCategory(),
                entity.getPrice(), ProductStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt().toInstant(ZoneOffset.UTC), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}

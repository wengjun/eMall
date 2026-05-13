package com.emall.review.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.review.domain.ProductReview;
import com.emall.review.domain.ReviewStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusReviewRepository implements ReviewRepository {
    private final ProductReviewMapper productReviewMapper;

    public MybatisPlusReviewRepository(ProductReviewMapper productReviewMapper) {
        this.productReviewMapper = productReviewMapper;
    }

    @Override
    public ProductReview save(ProductReview review) {
        ProductReviewEntity entity = toEntity(review);
        try {
            productReviewMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            productReviewMapper.update(null, new UpdateWrapper<ProductReviewEntity>()
                    .set("content", entity.getContent())
                    .set("status", entity.getStatus())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("review_id", entity.getReviewId()));
        }
        return review;
    }

    @Override
    public Optional<ProductReview> findById(long reviewId) {
        return Optional.ofNullable(productReviewMapper.selectById(reviewId)).map(this::toDomain);
    }

    @Override
    public List<ProductReview> findBySkuId(long skuId, int limit) {
        return productReviewMapper.selectList(new QueryWrapper<ProductReviewEntity>()
                .eq("sku_id", skuId)
                .eq("status", ReviewStatus.PUBLISHED.name())
                .orderByDesc("created_at")
                .last("LIMIT " + limit)).stream().map(this::toDomain).toList();
    }

    private ProductReviewEntity toEntity(ProductReview review) {
        ProductReviewEntity entity = new ProductReviewEntity();
        entity.setReviewId(review.reviewId());
        entity.setOrderId(review.orderId());
        entity.setSkuId(review.skuId());
        entity.setUserId(review.userId());
        entity.setRating(review.rating());
        entity.setContent(review.content());
        entity.setStatus(review.status().name());
        entity.setCreatedAt(LocalDateTime.ofInstant(review.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(review.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private ProductReview toDomain(ProductReviewEntity entity) {
        return new ProductReview(entity.getReviewId(), entity.getOrderId(), entity.getSkuId(), entity.getUserId(),
                entity.getRating(), entity.getContent(), ReviewStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt().toInstant(ZoneOffset.UTC), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}

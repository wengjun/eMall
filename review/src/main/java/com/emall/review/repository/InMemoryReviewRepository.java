package com.emall.review.repository;

import com.emall.review.domain.ProductReview;
import com.emall.review.domain.ReviewStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryReviewRepository implements ReviewRepository {
    private final ConcurrentMap<Long, ProductReview> reviews = new ConcurrentHashMap<>();

    @Override
    public ProductReview save(ProductReview review) {
        reviews.put(review.reviewId(), review);
        return review;
    }

    @Override
    public Optional<ProductReview> findById(long reviewId) {
        return Optional.ofNullable(reviews.get(reviewId));
    }

    @Override
    public List<ProductReview> findBySkuId(long skuId, int limit) {
        return reviews.values().stream()
                .filter(review -> review.skuId() == skuId)
                .filter(review -> review.status() == ReviewStatus.PUBLISHED)
                .sorted(Comparator.comparing(ProductReview::createdAt).reversed())
                .limit(limit)
                .toList();
    }
}

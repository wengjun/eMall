package com.emall.review.domain;

import java.time.Instant;

public record ProductReview(long reviewId, long orderId, long skuId, long userId, int rating, String content,
        ReviewStatus status, Instant createdAt, Instant updatedAt) {
    public ProductReview publish() {
        return new ProductReview(reviewId, orderId, skuId, userId, rating, content, ReviewStatus.PUBLISHED, createdAt,
                Instant.now());
    }

    public ProductReview reject() {
        return new ProductReview(reviewId, orderId, skuId, userId, rating, content, ReviewStatus.REJECTED, createdAt,
                Instant.now());
    }
}

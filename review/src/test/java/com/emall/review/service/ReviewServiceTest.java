package com.emall.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.review.domain.ProductReview;
import com.emall.review.domain.ReviewStatus;
import com.emall.review.repository.InMemoryReviewRepository;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {
    private final ReviewService reviewService = new ReviewService(
            new InMemoryReviewRepository(),
            new SnowflakeIdGenerator(5));

    @Test
    void shouldPublishPendingReviewAndListBySku() {
        ProductReview created = reviewService.create(90001L, 30001L, 70001L, 5, "excellent product");
        ProductReview published = reviewService.publish(created.reviewId());

        assertThat(published.status()).isEqualTo(ReviewStatus.PUBLISHED);
        assertThat(reviewService.listBySku(30001L, 10)).singleElement().satisfies(review -> {
            assertThat(review.reviewId()).isEqualTo(created.reviewId());
            assertThat(review.rating()).isEqualTo(5);
        });
    }

    @Test
    void shouldRejectInvalidRating() {
        assertThatThrownBy(() -> reviewService.create(90001L, 30001L, 70001L, 6, "invalid"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rating must be between 1 and 5");
    }
}

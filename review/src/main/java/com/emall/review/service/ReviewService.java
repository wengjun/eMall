package com.emall.review.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.review.domain.ProductReview;
import com.emall.review.domain.ReviewStatus;
import com.emall.review.repository.ReviewRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final SnowflakeIdGenerator idGenerator;

    public ReviewService(ReviewRepository reviewRepository, SnowflakeIdGenerator idGenerator) {
        this.reviewRepository = reviewRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public ProductReview create(long orderId, long skuId, long userId, int rating, String content) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "rating must be between 1 and 5");
        }
        Instant now = Instant.now();
        return reviewRepository.save(new ProductReview(idGenerator.nextId(), orderId, skuId, userId,
                rating, content, ReviewStatus.PENDING, now, now));
    }

    public ProductReview get(long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "review not found"));
    }

    public List<ProductReview> listBySku(long skuId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return reviewRepository.findBySkuId(skuId, safeLimit);
    }

    @Transactional
    public ProductReview publish(long reviewId) {
        ProductReview review = get(reviewId);
        if (review.status() != ReviewStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "review cannot be published from " + review.status());
        }
        return reviewRepository.save(review.publish());
    }

    @Transactional
    public ProductReview reject(long reviewId) {
        ProductReview review = get(reviewId);
        if (review.status() != ReviewStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "review cannot be rejected from " + review.status());
        }
        return reviewRepository.save(review.reject());
    }
}

package com.emall.review.repository;

import com.emall.review.domain.ProductReview;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository {
    ProductReview save(ProductReview review);

    Optional<ProductReview> findById(long reviewId);

    List<ProductReview> findBySkuId(long skuId, int limit);
}

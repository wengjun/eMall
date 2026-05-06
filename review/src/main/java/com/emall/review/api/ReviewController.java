package com.emall.review.api;

import com.emall.common.api.ApiResponse;
import com.emall.review.domain.ProductReview;
import com.emall.review.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductReview> create(@Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.ok(reviewService.create(
                request.orderId(), request.skuId(), request.userId(), request.rating(), request.content()));
    }

    @GetMapping("/{reviewId}")
    public ApiResponse<ProductReview> get(@PathVariable long reviewId) {
        return ApiResponse.ok(reviewService.get(reviewId));
    }

    @GetMapping("/products/{skuId}")
    public ApiResponse<List<ProductReview>> listBySku(@PathVariable long skuId,
                                                      @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(reviewService.listBySku(skuId, limit));
    }

    @PostMapping("/{reviewId}/publish")
    public ApiResponse<ProductReview> publish(@PathVariable long reviewId) {
        return ApiResponse.ok(reviewService.publish(reviewId));
    }

    @PostMapping("/{reviewId}/reject")
    public ApiResponse<ProductReview> reject(@PathVariable long reviewId) {
        return ApiResponse.ok(reviewService.reject(reviewId));
    }

    public record CreateReviewRequest(
            @Positive long orderId,
            @Positive long skuId,
            @Positive long userId,
            @Min(1) @Max(5) int rating,
            @NotBlank String content
    ) {
    }
}

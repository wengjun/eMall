package com.emall.marketing.api;

import com.emall.common.api.ApiResponse;
import com.emall.common.rpc.CouponReservationView;
import com.emall.marketing.domain.Coupon;
import com.emall.marketing.domain.PromotionQuote;
import com.emall.marketing.service.MarketingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketing")
public class MarketingController {
    private final MarketingService marketingService;

    public MarketingController(MarketingService marketingService) {
        this.marketingService = marketingService;
    }

    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Coupon> issue(@Valid @RequestBody IssueCouponRequest request) {
        return ApiResponse.ok(marketingService.issue(request.userId(), request.thresholdAmount(),
                request.discountAmount(), request.expiresAt()));
    }

    @GetMapping("/users/{userId}/coupons")
    public ApiResponse<List<Coupon>> list(@PathVariable long userId) {
        return ApiResponse.ok(marketingService.list(userId));
    }

    @PostMapping("/quotes")
    public ApiResponse<PromotionQuote> quote(@Valid @RequestBody PromotionQuoteRequest request) {
        return ApiResponse.ok(marketingService.quote(request.userId(), request.orderAmount()));
    }

    @PostMapping("/coupons/{couponId}/redeem")
    public ApiResponse<Coupon> redeem(@PathVariable String couponId, @Valid @RequestBody RedeemCouponRequest request) {
        return ApiResponse.ok(marketingService.redeem(couponId, request.orderAmount()));
    }

    @PostMapping("/coupons/{couponId}/reservations")
    public ApiResponse<CouponReservationView> reserveCoupon(@PathVariable String couponId,
            @Valid @RequestBody ReserveCouponRequest request) {
        return ApiResponse.ok(toReservationView(marketingService.reserveCoupon(request.reservationId(),
                request.userId(), couponId, request.orderAmount(), request.orderId())));
    }

    @PostMapping("/coupons/{couponId}/reservations/{reservationId}/confirm")
    public ApiResponse<CouponReservationView> confirmCoupon(@PathVariable String couponId,
            @PathVariable String reservationId, @Valid @RequestBody CouponReservationDecisionRequest request) {
        return ApiResponse
                .ok(toReservationView(marketingService.confirmCoupon(reservationId, couponId, request.orderId())));
    }

    @PostMapping("/coupons/{couponId}/reservations/{reservationId}/release")
    public ApiResponse<CouponReservationView> releaseCoupon(@PathVariable String couponId,
            @PathVariable String reservationId, @Valid @RequestBody CouponReservationDecisionRequest request) {
        return ApiResponse
                .ok(toReservationView(marketingService.releaseCoupon(reservationId, couponId, request.orderId())));
    }

    private CouponReservationView toReservationView(Coupon coupon) {
        return new CouponReservationView(coupon.reservationId(), coupon.userId(), coupon.couponId(),
                coupon.status().name(), coupon.discountAmount(), coupon.reservedOrderId(), coupon.updatedAt());
    }

    public record IssueCouponRequest(@Positive long userId, @NotNull @DecimalMin("0.00") BigDecimal thresholdAmount,
            @NotNull @DecimalMin("0.01") BigDecimal discountAmount, @NotNull @Future Instant expiresAt) {
    }

    public record PromotionQuoteRequest(@Positive long userId, @NotNull @DecimalMin("0.01") BigDecimal orderAmount) {
    }

    public record RedeemCouponRequest(@NotNull @DecimalMin("0.01") BigDecimal orderAmount) {
    }

    public record ReserveCouponRequest(@NotNull String reservationId, @Positive long userId,
            @NotNull @DecimalMin("0.01") BigDecimal orderAmount, @Positive long orderId) {
    }

    public record CouponReservationDecisionRequest(@Positive long orderId) {
    }
}

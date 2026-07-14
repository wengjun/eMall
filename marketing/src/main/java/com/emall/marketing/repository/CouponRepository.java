package com.emall.marketing.repository;

import com.emall.marketing.domain.Coupon;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface CouponRepository {
    Coupon save(Coupon coupon);

    Optional<Coupon> findById(String couponId);

    List<Coupon> findByUserId(long userId);

    boolean reserve(String couponId, Coupon coupon);

    boolean confirm(String couponId, String reservationId, long orderId, Coupon coupon);

    boolean release(String couponId, String reservationId, long orderId, Coupon coupon);

    List<Coupon> findExpiredReservations(Instant now, int limit);
}

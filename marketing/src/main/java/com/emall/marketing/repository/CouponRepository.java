package com.emall.marketing.repository;

import com.emall.marketing.domain.Coupon;
import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);

    Optional<Coupon> findById(String couponId);

    List<Coupon> findByUserId(long userId);
}

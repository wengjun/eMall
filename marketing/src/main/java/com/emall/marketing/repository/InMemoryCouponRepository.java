package com.emall.marketing.repository;

import com.emall.marketing.domain.Coupon;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryCouponRepository implements CouponRepository {
    private final ConcurrentMap<String, Coupon> coupons = new ConcurrentHashMap<>();

    @Override
    public Coupon save(Coupon coupon) {
        coupons.put(coupon.couponId(), coupon);
        return coupon;
    }

    @Override
    public Optional<Coupon> findById(String couponId) {
        return Optional.ofNullable(coupons.get(couponId));
    }

    @Override
    public List<Coupon> findByUserId(long userId) {
        return coupons.values().stream().filter(coupon -> coupon.userId() == userId)
                .sorted(Comparator.comparing(Coupon::updatedAt).reversed()).toList();
    }
}

package com.emall.marketing.repository;

import com.emall.marketing.domain.Coupon;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.time.Instant;
import com.emall.marketing.domain.CouponStatus;
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

    @Override
    public boolean reserve(String couponId, Coupon coupon) {
        return replace(couponId, CouponStatus.AVAILABLE, null, 0L, coupon);
    }

    @Override
    public boolean confirm(String couponId, String reservationId, long orderId, Coupon coupon) {
        return replace(couponId, CouponStatus.RESERVED, reservationId, orderId, coupon);
    }

    @Override
    public boolean release(String couponId, String reservationId, long orderId, Coupon coupon) {
        return replace(couponId, CouponStatus.RESERVED, reservationId, orderId, coupon);
    }

    @Override
    public List<Coupon> findExpiredReservations(Instant now, int limit) {
        return coupons.values().stream().filter(coupon -> coupon.reservationExpired(now))
                .sorted(Comparator.comparing(Coupon::reservedUntil)).limit(limit).toList();
    }

    private boolean replace(String couponId, CouponStatus expectedStatus, String reservationId, long orderId,
            Coupon replacement) {
        AtomicFlag updated = new AtomicFlag();
        coupons.computeIfPresent(couponId, (ignored, current) -> {
            boolean reservationMatches = reservationId == null || current.reservationMatches(reservationId, orderId);
            if (current.status() == expectedStatus && reservationMatches) {
                updated.value = true;
                return replacement;
            }
            return current;
        });
        return updated.value;
    }

    private static final class AtomicFlag {
        private boolean value;
    }
}

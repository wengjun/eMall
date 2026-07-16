package com.emall.marketing.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.sharding.ShardRouteIndex;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.marketing.domain.Coupon;
import com.emall.marketing.domain.CouponStatus;
import com.emall.marketing.domain.PromotionQuote;
import com.emall.marketing.repository.CouponRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingService {
    private final CouponRepository couponRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final BusinessMetrics businessMetrics;
    private final ShardRoutingOperations shardRoutingOperations;
    private final ShardRouteIndex shardRouteIndex;

    public MarketingService(CouponRepository couponRepository, SnowflakeIdGenerator idGenerator) {
        this(couponRepository, idGenerator, BusinessMetrics.noop(), ShardRoutingOperations.noop(),
                ShardRouteIndex.local());
    }

    @Autowired
    public MarketingService(CouponRepository couponRepository, SnowflakeIdGenerator idGenerator,
            BusinessMetrics businessMetrics, ShardRoutingOperations shardRoutingOperations,
            ShardRouteIndex shardRouteIndex) {
        this.couponRepository = couponRepository;
        this.idGenerator = idGenerator;
        this.businessMetrics = businessMetrics;
        this.shardRoutingOperations = shardRoutingOperations;
        this.shardRouteIndex = shardRouteIndex;
    }

    @Transactional
    public Coupon issue(long userId, BigDecimal thresholdAmount, BigDecimal discountAmount, Instant expiresAt) {
        if (discountAmount.signum() <= 0 || thresholdAmount.signum() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "invalid coupon amounts");
        }
        return shardRoutingOperations.execute("coupon", userId, () -> {
            Coupon coupon = new Coupon("coupon-" + idGenerator.nextId(), userId, thresholdAmount, discountAmount,
                    CouponStatus.AVAILABLE, expiresAt, Instant.now());
            Coupon saved = couponRepository.save(coupon);
            shardRouteIndex.bindUniqueTransactional("coupon", saved.couponId(), userId);
            return saved;
        });
    }

    public List<Coupon> list(long userId) {
        return shardRoutingOperations.executeRead("coupon", userId, () -> couponRepository.findByUserId(userId));
    }

    public Coupon getCoupon(String couponId) {
        return executeReadByCouponId(couponId, () -> requireCoupon(couponId));
    }

    public PromotionQuote quote(long userId, BigDecimal orderAmount) {
        return shardRoutingOperations.executeRead("coupon", userId, () -> couponRepository.findByUserId(userId).stream()
                .filter(coupon -> coupon.usable(orderAmount, Instant.now()))
                .max(Comparator.comparing(Coupon::discountAmount)).map(coupon -> quoteWithCoupon(coupon, orderAmount))
                .orElseGet(() -> PromotionQuote.none(userId, orderAmount)));
    }

    @Transactional
    public Coupon redeem(String couponId, BigDecimal orderAmount) {
        return executeByCouponId(couponId, () -> redeemInShard(couponId, orderAmount));
    }

    private Coupon redeemInShard(String couponId, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "coupon not found"));
        if (!coupon.usable(orderAmount, Instant.now())) {
            throw new BusinessException(ErrorCode.CONFLICT, "coupon is not usable");
        }
        return couponRepository.save(coupon.used());
    }

    @Transactional
    public Coupon reserveCoupon(String reservationId, long userId, String couponId, BigDecimal orderAmount,
            long orderId) {
        return shardRoutingOperations.execute("coupon", userId,
                () -> reserveCouponInShard(reservationId, userId, couponId, orderAmount, orderId));
    }

    private Coupon reserveCouponInShard(String reservationId, long userId, String couponId, BigDecimal orderAmount,
            long orderId) {
        Coupon coupon = requireCoupon(couponId);
        if (coupon.userId() != userId) {
            throw new BusinessException(ErrorCode.CONFLICT, "coupon owner mismatch");
        }
        if (coupon.reservedBy(reservationId, orderId)) {
            return coupon;
        }
        if (!coupon.usable(orderAmount, Instant.now())) {
            throw new BusinessException(ErrorCode.CONFLICT, "coupon is not reservable");
        }
        Instant reservedUntil = Instant.now().plus(Duration.ofMinutes(15));
        if (reservedUntil.isAfter(coupon.expiresAt())) {
            reservedUntil = coupon.expiresAt();
        }
        Coupon reserved = coupon.reserved(reservationId, orderId, reservedUntil);
        if (!couponRepository.reserve(couponId, reserved)) {
            Coupon concurrent = requireCoupon(couponId);
            if (concurrent.reservedBy(reservationId, orderId)) {
                return concurrent;
            }
            throw new BusinessException(ErrorCode.CONFLICT, "coupon was reserved concurrently");
        }
        businessMetrics.increment(BusinessMetricNames.COUPON_RESERVED);
        return reserved;
    }

    @Transactional
    public Coupon confirmCoupon(String reservationId, String couponId, long orderId) {
        return executeByCouponId(couponId, () -> confirmCouponInShard(reservationId, couponId, orderId));
    }

    private Coupon confirmCouponInShard(String reservationId, String couponId, long orderId) {
        Coupon coupon = requireCoupon(couponId);
        if (coupon.status() == CouponStatus.USED && coupon.reservationMatches(reservationId, orderId)) {
            return coupon;
        }
        if (!coupon.reservedBy(reservationId, orderId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "coupon reservation mismatch");
        }
        Coupon used = coupon.used();
        if (!couponRepository.confirm(couponId, reservationId, orderId, used)) {
            Coupon concurrent = requireCoupon(couponId);
            if (concurrent.status() == CouponStatus.USED && concurrent.reservationMatches(reservationId, orderId)) {
                return concurrent;
            }
            throw new BusinessException(ErrorCode.CONFLICT, "coupon reservation changed during confirmation");
        }
        businessMetrics.increment(BusinessMetricNames.COUPON_CONFIRMED);
        return used;
    }

    @Transactional
    public Coupon releaseCoupon(String reservationId, String couponId, long orderId) {
        return executeByCouponId(couponId, () -> releaseCouponInShard(reservationId, couponId, orderId));
    }

    private Coupon releaseCouponInShard(String reservationId, String couponId, long orderId) {
        Coupon coupon = requireCoupon(couponId);
        if (coupon.status() == CouponStatus.AVAILABLE) {
            return coupon;
        }
        if (!coupon.reservedBy(reservationId, orderId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "coupon reservation mismatch");
        }
        Coupon released = coupon.released();
        if (!couponRepository.release(couponId, reservationId, orderId, released)) {
            Coupon concurrent = requireCoupon(couponId);
            if (concurrent.status() == CouponStatus.AVAILABLE) {
                return concurrent;
            }
            throw new BusinessException(ErrorCode.CONFLICT, "coupon reservation changed during release");
        }
        businessMetrics.increment(BusinessMetricNames.COUPON_RELEASED);
        return released;
    }

    public int releaseExpiredReservations(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        return couponRepository.findExpiredReservations(Instant.now(), boundedLimit).stream().map(
                coupon -> releaseCouponInShard(coupon.reservationId(), coupon.couponId(), coupon.reservedOrderId()))
                .toList().size();
    }

    private <T> T executeByCouponId(String couponId, java.util.function.Supplier<T> action) {
        long userId = shardRouteIndex.resolveRequired("coupon", couponId, couponId.hashCode());
        return shardRoutingOperations.execute("coupon", userId, action);
    }

    private <T> T executeReadByCouponId(String couponId, java.util.function.Supplier<T> action) {
        long userId = shardRouteIndex.resolveRequired("coupon", couponId, couponId.hashCode());
        return shardRoutingOperations.executeRead("coupon", userId, action);
    }

    private Coupon requireCoupon(String couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "coupon not found"));
    }

    private PromotionQuote quoteWithCoupon(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount = coupon.discountAmount().min(orderAmount);
        return new PromotionQuote(coupon.userId(), orderAmount, discount, orderAmount.subtract(discount),
                coupon.couponId(), Instant.now());
    }
}

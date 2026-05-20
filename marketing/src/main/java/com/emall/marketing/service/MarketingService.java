package com.emall.marketing.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.marketing.domain.Coupon;
import com.emall.marketing.domain.CouponStatus;
import com.emall.marketing.domain.PromotionQuote;
import com.emall.marketing.repository.CouponRepository;
import java.math.BigDecimal;
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

    public MarketingService(CouponRepository couponRepository, SnowflakeIdGenerator idGenerator) {
        this(couponRepository, idGenerator, BusinessMetrics.noop());
    }

    @Autowired
    public MarketingService(CouponRepository couponRepository, SnowflakeIdGenerator idGenerator,
            BusinessMetrics businessMetrics) {
        this.couponRepository = couponRepository;
        this.idGenerator = idGenerator;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public Coupon issue(long userId, BigDecimal thresholdAmount, BigDecimal discountAmount, Instant expiresAt) {
        if (discountAmount.signum() <= 0 || thresholdAmount.signum() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "invalid coupon amounts");
        }
        Coupon coupon = new Coupon("coupon-" + idGenerator.nextId(), userId, thresholdAmount, discountAmount,
                CouponStatus.AVAILABLE, expiresAt, Instant.now());
        return couponRepository.save(coupon);
    }

    public List<Coupon> list(long userId) {
        return couponRepository.findByUserId(userId);
    }

    public PromotionQuote quote(long userId, BigDecimal orderAmount) {
        return couponRepository.findByUserId(userId).stream()
                .filter(coupon -> coupon.usable(orderAmount, Instant.now()))
                .max(Comparator.comparing(Coupon::discountAmount)).map(coupon -> quoteWithCoupon(coupon, orderAmount))
                .orElseGet(() -> PromotionQuote.none(userId, orderAmount));
    }

    @Transactional
    public Coupon redeem(String couponId, BigDecimal orderAmount) {
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
        Coupon reserved = couponRepository.save(coupon.reserved(reservationId, orderId));
        businessMetrics.increment(BusinessMetricNames.COUPON_RESERVED);
        return reserved;
    }

    @Transactional
    public Coupon confirmCoupon(String reservationId, String couponId, long orderId) {
        Coupon coupon = requireCoupon(couponId);
        if (coupon.status() == CouponStatus.USED && coupon.reservationMatches(reservationId, orderId)) {
            return coupon;
        }
        if (!coupon.reservedBy(reservationId, orderId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "coupon reservation mismatch");
        }
        Coupon used = couponRepository.save(coupon.used());
        businessMetrics.increment(BusinessMetricNames.COUPON_CONFIRMED);
        return used;
    }

    @Transactional
    public Coupon releaseCoupon(String reservationId, String couponId, long orderId) {
        Coupon coupon = requireCoupon(couponId);
        if (coupon.status() == CouponStatus.AVAILABLE) {
            return coupon;
        }
        if (!coupon.reservedBy(reservationId, orderId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "coupon reservation mismatch");
        }
        Coupon released = couponRepository.save(coupon.released());
        businessMetrics.increment(BusinessMetricNames.COUPON_RELEASED);
        return released;
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

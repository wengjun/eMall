package com.emall.marketing.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.marketing.domain.Coupon;
import com.emall.marketing.domain.CouponStatus;
import com.emall.marketing.domain.PromotionQuote;
import com.emall.marketing.repository.CouponRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingService {
    private final CouponRepository couponRepository;
    private final SnowflakeIdGenerator idGenerator;

    public MarketingService(CouponRepository couponRepository, SnowflakeIdGenerator idGenerator) {
        this.couponRepository = couponRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public Coupon issue(long userId, BigDecimal thresholdAmount, BigDecimal discountAmount, Instant expiresAt) {
        if (discountAmount.signum() <= 0 || thresholdAmount.signum() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "invalid coupon amounts");
        }
        Coupon coupon = new Coupon("coupon-" + idGenerator.nextId(), userId, thresholdAmount,
                discountAmount, CouponStatus.AVAILABLE, expiresAt, Instant.now());
        return couponRepository.save(coupon);
    }

    public List<Coupon> list(long userId) {
        return couponRepository.findByUserId(userId);
    }

    public PromotionQuote quote(long userId, BigDecimal orderAmount) {
        return couponRepository.findByUserId(userId).stream()
                .filter(coupon -> coupon.usable(orderAmount, Instant.now()))
                .max(Comparator.comparing(Coupon::discountAmount))
                .map(coupon -> quoteWithCoupon(coupon, orderAmount))
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

    private PromotionQuote quoteWithCoupon(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount = coupon.discountAmount().min(orderAmount);
        return new PromotionQuote(coupon.userId(), orderAmount, discount,
                orderAmount.subtract(discount), coupon.couponId(), Instant.now());
    }
}

package com.emall.marketing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.marketing.domain.Coupon;
import com.emall.marketing.domain.CouponStatus;
import com.emall.marketing.domain.PromotionQuote;
import com.emall.marketing.repository.InMemoryCouponRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MarketingServiceTest {
    private final MarketingService marketingService =
            new MarketingService(new InMemoryCouponRepository(), new SnowflakeIdGenerator(4));

    @Test
    void shouldApplyBestCouponAndRedeemIt() {
        marketingService.issue(70001L, new BigDecimal("100.00"), new BigDecimal("5.00"),
                Instant.now().plusSeconds(3600));
        Coupon bestCoupon = marketingService.issue(70001L, new BigDecimal("100.00"), new BigDecimal("20.00"),
                Instant.now().plusSeconds(3600));

        PromotionQuote quote = marketingService.quote(70001L, new BigDecimal("150.00"));
        Coupon redeemed = marketingService.redeem(bestCoupon.couponId(), new BigDecimal("150.00"));
        PromotionQuote afterRedeem = marketingService.quote(70001L, new BigDecimal("150.00"));

        assertThat(quote.couponId()).isEqualTo(bestCoupon.couponId());
        assertThat(quote.payableAmount()).isEqualByComparingTo("130.00");
        assertThat(redeemed.status()).isEqualTo(CouponStatus.USED);
        assertThat(afterRedeem.discountAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    void shouldReserveConfirmAndReleaseCouponWithIdempotentReservation() {
        Coupon coupon = marketingService.issue(70001L, new BigDecimal("100.00"), new BigDecimal("20.00"),
                Instant.now().plusSeconds(3600));

        Coupon reserved = marketingService.reserveCoupon("reservation-001", 70001L, coupon.couponId(),
                new BigDecimal("150.00"), 90001L);
        Coupon duplicateReservation = marketingService.reserveCoupon("reservation-001", 70001L, coupon.couponId(),
                new BigDecimal("150.00"), 90001L);
        Coupon used = marketingService.confirmCoupon("reservation-001", coupon.couponId(), 90001L);
        Coupon duplicateConfirm = marketingService.confirmCoupon("reservation-001", coupon.couponId(), 90001L);

        assertThat(reserved.status()).isEqualTo(CouponStatus.RESERVED);
        assertThat(duplicateReservation).isEqualTo(reserved);
        assertThat(used.status()).isEqualTo(CouponStatus.USED);
        assertThat(duplicateConfirm).isEqualTo(used);
    }

    @Test
    void shouldRejectConcurrentCouponReservationAndReleaseValidReservation() {
        Coupon coupon = marketingService.issue(70001L, new BigDecimal("100.00"), new BigDecimal("20.00"),
                Instant.now().plusSeconds(3600));

        marketingService.reserveCoupon("reservation-001", 70001L, coupon.couponId(), new BigDecimal("150.00"), 90001L);

        assertThatThrownBy(() -> marketingService.reserveCoupon("reservation-002", 70001L, coupon.couponId(),
                new BigDecimal("150.00"), 90002L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("coupon is not reservable");

        Coupon released = marketingService.releaseCoupon("reservation-001", coupon.couponId(), 90001L);

        assertThat(released.status()).isEqualTo(CouponStatus.AVAILABLE);
    }
}

package com.emall.marketing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.marketing.domain.Coupon;
import com.emall.marketing.domain.CouponStatus;
import com.emall.marketing.domain.PromotionQuote;
import com.emall.marketing.repository.InMemoryCouponRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MarketingServiceTest {
    private final MarketingService marketingService = new MarketingService(
            new InMemoryCouponRepository(),
            new SnowflakeIdGenerator(4));

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
}

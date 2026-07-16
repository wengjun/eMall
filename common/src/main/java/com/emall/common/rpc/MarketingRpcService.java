package com.emall.common.rpc;

public interface MarketingRpcService {
    PromotionQuoteView quote(PromotionQuoteCommand command);

    CouponReservationView reserveCoupon(CouponReservationCommand command);

    CouponReservationView getCoupon(String couponId);

    CouponReservationView confirmCoupon(CouponConfirmationCommand command);

    CouponReservationView releaseCoupon(CouponReleaseCommand command);
}

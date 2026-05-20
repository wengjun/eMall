package com.emall.marketing.rpc;

import com.emall.common.rpc.CouponConfirmationCommand;
import com.emall.common.rpc.CouponReleaseCommand;
import com.emall.common.rpc.CouponReservationCommand;
import com.emall.common.rpc.CouponReservationView;
import com.emall.common.rpc.MarketingRpcService;
import com.emall.common.rpc.PromotionQuoteCommand;
import com.emall.common.rpc.PromotionQuoteView;
import com.emall.marketing.domain.Coupon;
import com.emall.marketing.domain.PromotionQuote;
import com.emall.marketing.service.MarketingService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@DubboService
@ConditionalOnProperty(name = "emall.rpc.protocol", havingValue = "dubbo")
public class MarketingDubboService implements MarketingRpcService {
    private final MarketingService marketingService;

    public MarketingDubboService(MarketingService marketingService) {
        this.marketingService = marketingService;
    }

    @Override
    public PromotionQuoteView quote(PromotionQuoteCommand command) {
        PromotionQuote quote = marketingService.quote(command.userId(), command.orderAmount());
        return new PromotionQuoteView(quote.userId(), quote.orderAmount(), quote.discountAmount(),
                quote.payableAmount(), quote.couponId(), quote.quotedAt());
    }

    @Override
    public CouponReservationView reserveCoupon(CouponReservationCommand command) {
        return toView(marketingService.reserveCoupon(command.reservationId(), command.userId(), command.couponId(),
                command.orderAmount(), command.orderId()));
    }

    @Override
    public CouponReservationView confirmCoupon(CouponConfirmationCommand command) {
        return toView(marketingService.confirmCoupon(command.reservationId(), command.couponId(), command.orderId()));
    }

    @Override
    public CouponReservationView releaseCoupon(CouponReleaseCommand command) {
        return toView(marketingService.releaseCoupon(command.reservationId(), command.couponId(), command.orderId()));
    }

    private CouponReservationView toView(Coupon coupon) {
        return new CouponReservationView(coupon.reservationId(), coupon.userId(), coupon.couponId(),
                coupon.status().name(), coupon.discountAmount(), coupon.reservedOrderId(), coupon.updatedAt());
    }
}
